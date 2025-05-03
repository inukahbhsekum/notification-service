(ns messages.validation
  (:require [clojure.tools.logging :as ctl]
            [utils.convertor-utils :as ucu]
            [messages.models :as mm]
            [messages.schema :as ms]
            [schema.core :as sc]
            [user-details.models :as udm])
  (:import java.util.UUID))


(defn validate-message-creation-request
  [{:keys [request-body params]} dependencies]
  (try
    (let [user-id (:user_id params)
          user-details (udm/fetch-user-details user-id dependencies)
          valid-payload (sc/validate ms/CreateMessageRequest request-body)
          sender-details (udm/fetch-user-details (:sender valid-payload)
                                                 dependencies)
          topic-receiver (udm/fetch-notification-topic-receiver (:topic_id valid-payload)
                                                                (:receiver valid-payload)
                                                                dependencies)
          invalid-reciever? (nil? topic-receiver)]
      (cond
        invalid-reciever?
        (throw (Exception. "Message reciever is not associated with the topic"))
        (= (:user-type user-details) "receiver")
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


(defn validate-send-message-request
  [{:keys [request-body]} dependencies]
  (try
    (let [valid-payload (sc/validate ms/SendMessageRequest
                                     request-body)
          sender-details (udm/fetch-user-details (:sender_id valid-payload)
                                                 dependencies)
          message-details (mm/fetch-message-by-message-id (:message_id valid-payload))]
      (cond
        (and (not= (:user-type sender-details) "manager")
             (not= (:user-type sender-details) "publisher"))
        (throw (Exception. "Message can be sent by manager or publisher"))
        (nil? message-details)
        (throw (Exception. "Invalid message_id passed"))
        :else
        (assoc valid-payload :message_details message-details)))
    (catch Exception e
      (ctl/error "Invalid send message request" request-body (ex-message e))
      (throw (Exception. "Invalid send message request")))))


(defn validate-fetch-message-request
  [{:keys [request-body]} dependencies]
  (try
    (let [valid-payload (sc/validate ms/FetchMessageRequest request-body)
          user-id (:user_id valid-payload)
          user-details (udm/fetch-user-details user-id
                                               dependencies)
          from-timestamp (ucu/millisecond-to-datetime (:from valid-payload))]
      (if (nil? user-details)
        (throw (Exception. "Invalid user-id passed in the request"))
        (assoc valid-payload
               :from from-timestamp
               :user_id user-id)))
    (catch Exception e
      (ctl/error "Invalid fetch messages request" request-body (ex-message e))
      (throw (Exception. "Invalid fetch message request")))))
