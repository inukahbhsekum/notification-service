(ns utils.response-utils
  (:require [clojure.data.json :as json]))


(defn response
  ([status]
   (response status nil))
  ([status body]
   (merge
     {:status  status
      :headers {"Content-type" "application/json"}}
     (when body {:body (json/write-str body)}))))


(def ok (partial response 200))
(def not-found (partial response 404))
(def created (partial response 201))
(def failed (partial response 400))
