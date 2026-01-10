(ns user-details.factory
  (:require [clojure.tools.logging :as ctl]))

(defn logging-alert-decorator
  "It is a decorator function that wraps around the handler function to
   provide logging and alerting capabilities. It takes a handler function
   as an argument and returns a new function that adds logging and alerting
   functionality."
  [handler]
  (fn [request-payload dependencies]
    (try
      (handler request-payload dependencies)
      (catch Exception e
        (ctl/error (ex-message e))
        (throw e)))))
