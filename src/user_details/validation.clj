(ns user-details.validation
  (:require [buddy.hashers :as hashers]
            [clojure.tools.logging :as ctl]
            [malli.core :as m]
            [user-details.factory :refer [logging-alert-decorator]]
            [user-details.models :as udm]
            [user-details.schema :as uds])
  (:import (java.util UUID)))

(def user-creation-validator (m/validator uds/CreateUserRequest))
(def topic-creation-validator (m/validator uds/CreateTopicRequest))
(def user-topic-mapping-validator (m/validator uds/CreateTopicUserMappingRequest))
(def user-login-request-validator (m/validator uds/UserLoginRequest))

(defn validate-user-creation-request
  [{:keys [request-body params]}]
  (try
    (let [user-id (:user_id params)
          user-id (if user-id
                    (UUID/fromString user-id)
                    (UUID/randomUUID))
          valid? (user-creation-validator request-body)]
      (when (not valid?)
        (throw (Exception. "Invalid request payload")))
      (when valid?
        (if user-id
          (assoc request-body :user_id user-id)
          request-body)))
    (catch Exception e
      (ctl/error "Invalid register user request payload"
                 request-body
                 (ex-message e))
      (throw (Exception. "Invalid register user request payload")))))


(defn validate-topic-creation-request
  [{:keys [request-body params]} dependencies]
  (try
    (let [topic_id (:topic_id params)
          topic_id (if topic_id
                     (UUID/fromString topic_id)
                     (UUID/randomUUID))
          valid? (topic-creation-validator request-body)
          _ (when (not valid?)
              (throw (Exception. "Invalid request payload")))
          user-id (:user_id request-body)
          user-details (udm/fetch-user-details user-id dependencies)]
      (when (and (not= (:user-type user-details) "manager")
                 (not= (:user-type user-details) "publisher"))
        (throw (Exception. "User should be publisher or manager")))
      (assoc request-body :topic_id topic_id))
    (catch Exception e
      (ctl/error "Invalid create topic request payload"
                 request-body
                 (ex-message e))
      (throw (Exception. (str "Invalid create topic request, "
                              (ex-message e)))))))


(defn validate-topic-user-mapping-request
  [{:keys [request-body] :as request-payload} dependencies]
  (try
    (let [valid? (user-topic-mapping-validator request-body)
          _ (when (not valid?)
              (throw (Exception. "Invalid request body")))
          user-details (udm/fetch-user-details (:manager-id request-body)
                                               dependencies)]
      (when (not= (:user-type user-details) "manager")
        (throw (Exception. "User should be a manager for creating the mapping")))
      request-body)
    (catch Exception e
      (ctl/error "Invalid topic user request payload"
                 request-payload
                 (ex-message e))
      (throw (Exception. (str "Invalid topic user mapping request "
                              (ex-message e)))))))


(defn- validate-user-password
  "It takes the plain password passed during login
   and validates it as per the password hash provided"
  [request-plain-password pwd-hash]
  (hashers/verify request-plain-password pwd-hash))


(defn user-login-validator
  "It validates the user login request and does the following
   1. Validates the login-request payload as per schema
   2. Fetches the user-details from the username
   3. Checks the password in login request against the password
      in user-details in encrypted format
   Returns the request-payload on success password validation"
  [{:keys [request-body] :as request-payload} dependencies]
  (logging-alert-decorator
   (let [valid? (user-login-request-validator request-body)
         user-details (udm/fetch-user-details-from-username (:username request-body)
                                                            dependencies)
         pwd-valid? (validate-user-password (:password request-body)
                                            (:pwd-hash user-details))]
     (when (or (not valid?) (not pwd-valid?))
       (throw (Exception. "Invalid login credentials")))
     (assoc request-payload
            :user-details user-details))))
