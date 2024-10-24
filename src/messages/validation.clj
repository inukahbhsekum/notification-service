(ns messages.validation
  (:require [clojure.tools.logging :as ctl]
            [messages.schema :as ms]
            [schema.core :as sc]
            [user-details.models :as udm]))


(defn validate-message-creation-request
  [{:keys [request-body params]} dependencies]
  (try
    (let [user-id (:user_id params)
          user-details (udm/fetch-user-details user-id dependencies)
          valid-payload (sc/validate ms/CreateMessageRequest request-body)
          user-type (:user-type user-details)
          sender-details (udm/fetch-user-details user-id dependencies)]
      (cond
        (and (not= user-type "manager")
             (not= user-type "publisher"))
        (throw (Exception. "Message can be created by manager or publisher"))
        (not= (:user-type sender-details) "publisher")
        (throw (Exception. "Only publisher can send message"))
        (empty? (:message_text valid-payload))
        (throw (Exception. "Message text cannot be empty or null"))
        :else
        valid-payload))
    (catch Exception e
      (ctl/error "Invalid create message request" request-body (ex-message e))
      (throw (Exception. "Invalid create message request payload")))))
