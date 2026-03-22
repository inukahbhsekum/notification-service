(ns utils.response-utils
  (:require [clojure.walk :as walk]))


(defn response
  ([status]
   (response status nil))
  ([status body]
   {:status  status
    :headers {"Content-type" "application/json"}
    :body    (walk/stringify-keys body)}))


(def ok (partial response 200))
(def created (partial response 201))
(def updated (partial response 204))
(def not-found (partial response 404))
(def failed (partial response 400))
(def not-authorised (partial response 401))
