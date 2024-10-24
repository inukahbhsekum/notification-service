(ns messages.handler
  (:require [utils.response-utils :as ur]
            [messages.validation :as mv]
            [messages.models :as mm]))


(defn- create-message
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        (mv/validate-message-creation-request dependencies)
        (mm/create-or-update-message dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def create-message-handler
  {:name :create-message-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [user-details (create-message request dependencies)
           response (ur/ok user-details)]
       (assoc context :response response)))})
