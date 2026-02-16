(ns consumers.notification-message-consumer
  (:require [clojure.tools.logging :as ctl]
            [clojure.walk :refer [keywordize-keys]]
            [components.database-components :as cdc]
            [components.kafka-components :as ckc]
            [config :as config]
            [constants.notification-events :as cne]
            [messages.models :as mm]
            [websocket.handler :as wh])
  (:import [java.time Duration]
           (org.apache.kafka.clients.consumer ConsumerRecord)))

(def consumer-instance nil)
(defonce consumer-thread (atom nil))

(defmulti handle-event
  (fn [{:strs [medium-name] :as event}]
    (ctl/info "------> info" event)
    [(keyword medium-name) (-> (:event-type event)
                               cne/event-type-event-value>)]))


(defmethod handle-event [:websocket :recieve_message]
  [{:strs [message_id topic_id] :as event}]
  (ctl/info "----->" event)
  (let [db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        user-message (mm/fetch-user-message {:topic_id topic_id
                                             :message_id message_id}
                                            db-pool)]
    (mm/upsert-user-message-details {:message_id message_id
                                     :user_id (:user-id user-message)
                                     :topic_id topic_id
                                     :status "received"}
                                    db-pool)
    (wh/handle-received-message user-message db-pool)))


(defmethod handle-event :default
  [event]
  (ctl/error "Invalid event-type" event))


(defn start-consuming!
  []
  (try
    (while true
      (let [records (.poll consumer-instance (Duration/ofMillis 1000))]
        (doseq [^ConsumerRecord record records]
          (ctl/info "Record received: " (.value record))
          (-> (.value record)
              handle-event))))
    (catch Exception e
      (ctl/info "Exception while starting the consumer" (.getMessage e)))
    (finally
      (.close consumer-instance))))


(defn close-notification-consumer!
  []
  (when-let [t @consumer-thread]
    (ctl/info "Stopping notification-consumer ...")
    (.wakeup (:consumer t))
    (reset! consumer-thread nil)))


(defn -main
  []
  (let [config (config/read-config)
        consumer (ckc/create-consumer (:message-kafka-consumer-config config))]
    (alter-var-root #'consumer-instance (constantly consumer))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. close-notification-consumer!))
    (let [t (Thread. (fn []
                       (try
                         (start-consuming!)
                         (catch org.apache.kafka.common.errors.WakeupException e
                           (ctl/info "Consumer woke up for shutdown."
                                     (.getMessage e))))))]
      (reset! consumer-thread {:thread t :consumer consumer})
      (.start t)
      (ctl/info "Consumer started in background. REPL is free!"))))
