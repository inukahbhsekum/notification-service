(ns utils.response-utils
  (:require [clojure.data.json :as json]))


(defn response
  ([status]
   (response status nil))
  ([status body]
   {:status  status
    :headers {"Content-type" "application/json"}
    :body    (json/write-str body)}))


(def ok (partial response 200))
(def created (partial response 201))
(def updated (partial response 204))
(def not-found (partial response 404))
(def failed (partial response 400))
(def not-authorised (partial response 401))