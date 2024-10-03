(ns user-details.validation
  (:require [schema.core :as sc]
            [user-details.schema :as uds]))


(defn validate-user-creation-request
  [request-payload]
  (sc/validate uds/CreateUserRequest request-payload))
