(ns user-details.handlers
  (:require [utils.response-utils :as ur]
            [user-details.validation :as udv]
            [user-details.models :as udm]))


(defn- register-user
  [request dependencies]
  (let [params (:query-params request)
        request-body (:json-params request)]
    (-> (merge request-body params)
        udv/validate-user-creation-request
        (udm/create-user dependencies))))


(def register-user-handler
  {:name :register-user-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [user-details (register-user request dependencies)
           response (ur/ok user-details)]
       (assoc context :response response)))})
