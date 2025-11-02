(ns user-details.validation
  (:require [clojure.tools.logging :as ctl]
            [malli.core :as m]
            [user-details.models :as udm]
            [user-details.schema :as uds])
  (:import (java.util UUID)))

(def user-creation-validator (m/validator uds/CreateUserRequest))
(def topic-creation-validator (m/validator uds/CreateTopicRequest))
(def user-topic-mapping-validator (m/validator uds/CreateTopicUserMappingRequest))

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

(comment
  1. "fetch user from username"
  2. "encrypt the password"
  3. "check the equality"
  4. "update the availability"
  5. "update the availability atom"
  6. "update the assignment actions in future if any
      based on a flag"
  7. "Update current status in db as well in cache")

(defn user-login-validator
  [{:keys [request-body] :as request-payload} dependencies]
  (try
    (let [user-details (udm/fetch-user-details (:username request-body)
                                               dependencies)
          valid? ()]
      request-body)
    (catch Exception e
      (ctl/error "Invalid login credentials"
                 request-payload
                 (ex-message e))
      (throw (Exception. (str "Invalid login credentials" (ex-message e)))))))
