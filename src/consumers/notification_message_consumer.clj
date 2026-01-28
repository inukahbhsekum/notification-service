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

(defmulti handle-event
  (fn [{:keys [medium-name event_type]}]
    [(keyword medium-name) (cne/event-type-event-value> event_type)]))


(defmethod handle-event [:websocket :recieve_message]
  [{:keys [message_id topic_id] :as event}]
  (let [db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        users-message-details (mm/fetch-user-message {:topic_id topic_id
                                                      :message_id message_id}
                                                     db-pool)
        _ (def umd users-message-details)]
    (doseq [user-message-details users-message-details]
      (mm/upsert-user-message-details {:message_id message_id
                                       :user_id (:user_id user-message-details)
                                       :topic_id topic_id
                                       :status "received"}
                                      db-pool)
      (wh/handle-received-message user-message-details db-pool))))


(defmethod handle-event :default
  [_]
  (ctl/error "Invalid event-type"))


(defn create-notification-message-consumer
  [config]
  (ctl/info "Creating Notification Message Consumer..." config)
  (let [consumer (ckc/create-consumer (:message-kafka-consumer-config config))]
    (alter-var-root #'consumer-instance (constantly consumer))
    (while true
      (let [records (.poll consumer (Duration/ofMillis 1000))]
        (doseq [^ConsumerRecord record records]
          (-> (.value record)
              keywordize-keys
              handle-event))))
    consumer-instance))


(defn -main
  []
  (let [service-config (config/read-config)]
    (create-notification-message-consumer service-config)))
