(ns user-details.handlers
  (:require [clojure.tools.logging :as ctl]
            [utils.response-utils :as ur]
            [user-details.validation :as udv]))


(defn- register-user
  [request dependencies]
  (let [params (:query-params request)
        request-body (:json-params request)]
    (ctl/info "request input: " [params request-body])
    (-> (merge request-body params)
        udv/validate-user-creation-request)
    ;; pending complete function
    params))


(def register-user-handler
  {:name :register-user-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           user-details (register-user request dependencies)
           response (ur/ok user-details)]
       (assoc context :response response)))})
