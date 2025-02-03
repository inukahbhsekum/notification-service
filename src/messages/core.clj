(ns messages.core
  (:require [messages.models :as mm]
            [messages.validation :as mv]))


(defn send-message
  [{:keys [request-body]} dependencies]
  ;; create websocket connection and send message
  (let [topic-id (:topic_id request-body)
        message-text (:message_text request-body)
        manager-id (:manager_id request-body)]
    ()))


(defn update-message-activity-log
  [{:keys [request-body receiver]} dependencies]
  (let [receiver (mv/validate-receiver receiver)]
    ;; Rename keys of request-body to required keys
    (mm/update-message-activity-log (assoc request-body :receiver receiver)
                                    dependencies)))
