(ns messages.core)

(defn send-message
  [{:keys [request-body]} dependencies]
  (let [topic-id (:topic_id request-body)
        message-text (:message_text request-body)
        manager-id (:manager_id request-body)]
    ()))
