(ns messages.validation
  (:require [clojure.tools.logging :as ctl]
            [utils.convertor-utils :as ucu]
            [malli.core :as mc]
            [messages.models :as mm]
            [messages.schema :as ms]
            [user-details.models :as udm])
  (:import java.util.UUID))

(def create-message-validator (mc/validator ms/CreateMessageRequest))
(def send-message-validator (mc/validator ms/SendMessageRequest))
(def fetch-message-validator (mc/validator ms/FetchMessageRequest))

(defn validate-message-creation-request
  [{:keys [request-body params]} dependencies]
  (try
    (let [user-id (:user_id params)
          user-details (udm/fetch-user-details user-id dependencies)
          valid-payload? (create-message-validator request-body)
          _ (when (not valid-payload?)
              (throw (Exception. "Invalid create-message payload")))
          sender-details (udm/fetch-user-details (:sender request-body)
                                                 dependencies)
          topic-receiver (udm/fetch-notification-topic-receiver (:topic_id request-body)
                                                                (:receiver request-body)
                                                                dependencies)
          invalid-reciever? (nil? topic-receiver)
          message_medium (mm/fetch-message-medium (:message_medium request-body)
                                                  dependencies)
          update-request-body (merge request-body
                                     {:message_medium (:medium-id message_medium)})]
      (cond
        invalid-reciever?
        (throw (Exception. "Message reciever is not associated with the topic"))
        (= (:user-type user-details) "receiver")
        (throw (Exception. "Message can be created by manager or publisher"))
        (not= (:user-type sender-details) "publisher")
        (throw (Exception. "Only publisher can send message"))
        (empty? (:message_text request-body))
        (throw (Exception. "Message text cannot be empty or null"))
        (nil? message_medium)
        (throw (Exception. "Message medium is not supported"))
        :else
        update-request-body))
    (catch Exception e
      (ctl/error "Invalid create message request" {:body request-body
                                                   :error-message (ex-message e)
                                                   :exception e})
      (throw (Exception. "Invalid create message request payload")))))


(defn validate-send-message-request
  [{:keys [request-body]} dependencies]
  (try
    (let [valid-payload? (send-message-validator request-body)
          _ (when (not valid-payload?)
              (throw (Exception. "Invalid send message payload")))
          sender-details (udm/fetch-user-details (:sender_id request-body)
                                                 dependencies)
          message-details (mm/fetch-message-by-message-id (:message_id request-body)
                                                          dependencies)
          message-medium (mm/fetch-message-medium-by-id (:message-medium message-details)
                                                        dependencies)
          message-details (assoc message-details
                                 :medium-name (:medium-name message-medium))]
      (cond
        (and (not= (:user-type sender-details) "manager")
             (not= (:user-type sender-details) "publisher"))
        (throw (Exception. "Message can be sent by manager or publisher"))
        (nil? message-details)
        (throw (Exception. "Invalid message_id passed"))
        :else
        (assoc request-body
               :message_details message-details)))
    (catch Exception e
      (ctl/error "Invalid send message request" request-body (ex-message e))
      (throw (Exception. "Invalid send message request")))))


(defn validate-fetch-message-request
  [{:keys [request-body]} _]
  (try
    (let [valid-payload? (fetch-message-validator request-body)
          _ (when (not valid-payload?)
              (throw (Exception. "Invalid fetch-message payload")))
          topic-id (:topic_id request-body)
          from-timestamp (ucu/millisecond-to-datetime (:from request-body))]
      (assoc request-body
             :from from-timestamp
             :topic_id (UUID/fromString topic-id)))
    (catch Exception e
      (ctl/error "Invalid fetch messages request" request-body (ex-message e))
      (throw (Exception. "Invalid fetch message request")))))
