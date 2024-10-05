(ns user-details.validation
  (:require [clojure.tools.logging :as ctl]
            [schema.core :as sc]
            [user-details.schema :as uds]))


(defn validate-user-creation-request
  [request-payload]
  (ctl/info "request body: ", request-payload)
  (sc/validate uds/CreateUserRequest request-payload))
