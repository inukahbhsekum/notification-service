(ns messages.core
  (:require [messages.models :as mm]))


(defn send-message
  [{:keys [request-body]} dependencies]
  (let [topic-id (:topic_id request-body)
        message-text (:message_text request-body)
        manager-id (:manager_id request-body)]
    ()))


(defn update-message-activity-log
  [{:keys [request-body]} dependencies]
  (let []
    ;; Rename keys of request-body to required keys
    (mm/update-message-activity-log request-body dependencies)))
