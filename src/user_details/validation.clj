(ns user-details.validation
  (:require [clojure.tools.logging :as ctl]
            [schema.core :as sc]
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
                 request-body)
      (throw (Exception. "Invalid register user request payload")))))
