(ns messages.core
  (:require [clojure.data.json :as json]
            [messages.models :as mm]
            [producers.factory :as pf]
            [user-details.models :as udm]
            [utils.function-utils :as ufu])
  (:import [java.util UUID]))


(defn send-message
  [{:keys [message_id message_details]} {:keys [producer] :as dependencies}]
  (ufu/improper-thrush (json/write-str message_details)
                       (partial pf/send-event producer
                                (get-in dependencies [:config :message-kafka-topic] "ns_message")
                                message_id)))


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
                      :receivers receiver-ids
                      :message_medium (:message_medium message-details)}]
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
