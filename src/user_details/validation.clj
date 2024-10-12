(ns user-details.validation
  (:require [clojure.tools.logging :as ctl]
            [schema.core :as sc]
            [user-details.models :as udm]
            [user-details.schema :as uds])
  (:import (java.util UUID)))


(defn validate-user-creation-request
  [{:keys [request-body params] :as request-payload}]
  (ctl/info "register user request payload: " request-payload)
  (try
    (let [user-id (:user_id params)
          user-id (if user-id
                    (UUID/fromString user-id)
                    (UUID/randomUUID))
          valid-payload (sc/validate uds/CreateUserRequest request-body)]
      (if user-id
        (assoc valid-payload :user_id user-id)
        valid-payload))
    (catch Exception e
      (ctl/error "Invalid register user request payload"
                 request-body
                 (ex-message e))
      (throw (Exception. "Invalid register user request payload")))))


(defn validate-topic-creation-request
  [{:keys [request-body params] :as request-payload} dependencies]
  (ctl/info "create topic request payload: " request-payload)
  (try
    (let [topic_id (:topic_id params)
          topic_id (if topic_id
                     (UUID/fromString topic_id)
                     (UUID/randomUUID))
          valid-payload (sc/validate uds/CreateTopicRequest request-body)
          user-id (:user_id valid-payload)
          user-details (udm/fetch-user-details user-id dependencies)]
      (when (or (not= (:user_type user-details) "manager")
                (not= (:user_type user-details) "publisher"))
        (throw (Exception. "User should be publisher or manager")))
      (assoc valid-payload :topic_id topic_id))
    (catch Exception e
      (ctl/error "Invalid create topic request payload"
                 request-body
                 (ex-message e))
      (throw (Exception. (str "Invalid create topic request"
                              (ex-message e)))))))
