(ns consumers.notification-message-consumer
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as ctl]
            [clojure.walk :refer [keywordize-keys]]
            [components.database-components :as cdc]
            [config :as config]
            [constants.notification-events :as cne]
            [consumers.factory :as cf]
            [messages.models :as mm]
            [websocket.handler :as wh])
  (:import (java.util Properties)
           (java.util Collections Properties)
           (org.apache.kafka.clients.consumer ConsumerConfig
                                              ConsumerRecord
                                              KafkaConsumer)
           (java.time Duration)))

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

;; ---------------------------------------------------------------------||------------------------------------------------------------------------------||
;; ---------------------------------------------------------------------||------------------------------------------------------------------------------||

(defrecord NotificationConsumer [config state]
  cf/KafkaConsumer
  (start-consumer
    [this]
    (let [props (Properties.)]
      (.put props ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG (:bootstrap-servers config))
      (.put props ConsumerConfig/GROUP_ID_CONFIG (:group-id config))
      (.put props ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG
            "org.apache.kafka.common.serialization.StringDeserializer")
      (.put props ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG
            "org.apache.kafka.common.serialization.StringDeserializer")
      (let [consumer (KafkaConsumer. props)]
        (.subscribe consumer (Collections/singletonList (:topic-name config)))
        (reset! state {:consumer consumer}))
      (ctl/info "Consumer started in background. REPL is free!")
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (ctl/info "Shutdown hook triggered,
                                              closing notification-consumer..")
                                   (.wakeup (:consumer @state))
                                   (reset! state nil))))
      (-> (Thread. (fn []
                     (try
                       (cf/handle-consumer-event this)
                       (catch org.apache.kafka.common.errors.WakeupException e
                         (ctl/info "Consumer woke up for shutdown."
                                   (.getMessage e))))))
          (.start))))


  (stop-consumer
    [this]
    (when-let [consumer (:consumer @state)]
      (ctl/info "Stopping notification-consumer ...")
      (.wakeup consumer)
      (reset! state nil)))


  (handle-consumer-event
    [this]
    (try
      (while true
        (let [consumer-instance (:consumer @state)
              records (.poll consumer-instance (Duration/ofMillis 1000))]
          (doseq [^ConsumerRecord record records]
            (-> (.value record)
                json/read-str
                keywordize-keys
                handle-event))))
      (catch Exception e
        (ctl/info "Exception while starting the consumer" (.getMessage e)))
      (finally
        (.close (:consumer @state))
        (reset! state nil)))))


(defn -main
  []
  (let [config (config/read-config)]
    (->NotificationConsumer config
                            (atom nil))))
