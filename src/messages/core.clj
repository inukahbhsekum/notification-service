(ns messages.core
  (:require [messages.models :as mm]
            [user-details.models :as udm])
  (:import [java.util UUID]))


(defn send-message
  [{:keys [request-body]} dependencies]
  (let [message-id (:message_id request-body)
        sender_id (:sender_id request-body)
        message_details (:message_details request-body)]
    message_details))


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
