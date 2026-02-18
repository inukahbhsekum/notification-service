(ns consumers.notification-message-consumer
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as ctl]
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
  (fn [{:keys [medium-name event-type]}]
    [(keyword medium-name) (-> event-type
                               cne/event-type-event-value>)]))


(defmethod handle-event [:websocket :recieve_message]
  [{:keys [message-id topic-id]}]
  (let [db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        user-message (mm/fetch-user-message-details {:topic_id topic-id
                                                     :message_id message-id}
                                                    db-pool)
        user-message-status (:status user-message)]
    (cond
      (= user-message-status "recieved")
      (ctl/error "message already recieved")
      (= user-message-status "read")
      (ctl/error "message is already read")
      :else
      (do
        (mm/upsert-user-message-details {:message_id (str message-id)
                                         :user_id (-> :user-id
                                                      user-message
                                                      str)
                                         :topic_id (str topic-id)
                                         :status "recieved"}
                                        db-pool)
        (wh/handle-received-message user-message db-pool)))))


(defmethod handle-event :default
  [{:keys [event-type]}]
  (ctl/error "Invalid event-type" event-type))


(defn start-consuming!
  []
  (try
    (while true
      (let [records (.poll consumer-instance (Duration/ofMillis 1000))]
        (doseq [^ConsumerRecord record records]
          (-> (.value record)
              json/read-str
              keywordize-keys
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
