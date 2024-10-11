(ns user-details.handlers
  (:require [utils.response-utils :as ur]
            [user-details.validation :as udv]
            [user-details.models :as udm]))


(defn- register-user
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        udv/validate-user-creation-request
        (udm/create-or-update-user dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def register-user-handler
  {:name :register-user-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [user-details (register-user request dependencies)
           response (ur/ok user-details)]
       (assoc context :response response)))})
