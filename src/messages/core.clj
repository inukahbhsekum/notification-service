(ns messages.core
  (:require [components.kafka-components :as ckc]
            [config :as config]
            [messages.models :as mm]
            [user-details.models :as udm]
            [utils.function-utils :as ufu])
  (:import [java.util UUID]))


(defn send-message
  [{:keys [request-body]} {:keys [message-producer]}]
  (let [message-id (:message_id request-body)]
    (ufu/improper-thrush request-body
                         (partial ckc/send-message message-producer
                                  (:message-kafka-topic (config/read-config))
                                  message-id))))


(defn update-message-activity-log
  [message-details dependencies]
  (let [topic-id (:topic_id message-details)
        topic-receivers (udm/fetch-notification-topic-receivers topic-id dependencies)
        receiver-ids (map :user-id topic-receivers)
        activity-log {:sender (-> message-details
                                  :sender
                                  (UUID/fromString))
                      :topic_id (UUID/fromString topic-id)
                      :message_id (:message_id message-details)
                      :meta {:message_body (:message_text message-details)}
                      :created_at (:created_at message-details)
                      :receivers receiver-ids}]
    (mm/update-message-activity-log activity-log dependencies)))


(defn update-user-message-details
  [message-details dependencies]
  (let [topic-id (:topic_id message-details)
        topic-receivers (udm/fetch-notification-topic-receivers topic-id dependencies)
        receiver-ids (map :user-id topic-receivers)
        message-id (:message_id message-details)]
    (doseq [receiver-id receiver-ids]
      (mm/upsert-user-message-details {:user_id receiver-id
                                       :message_id message-id
                                       :topic_id topic-id
                                       :status "created"}
                                      dependencies))))
