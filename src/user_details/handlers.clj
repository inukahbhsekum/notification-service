(ns user-details.handlers
  (:require [clojure.tools.logging :as ctl]
            [utils.response-utils :as ur]))


(defn- register-user
  [{:keys [in-memory-state-component] :as dependencies} params]
  ;; pending implementation
  (ctl/log "----> " [dependencies params])
  params)


(def register-user-handler
  {:name :register-user-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           user-details (register-user dependencies
                                       (-> request
                                           :path-params))
           response (ur/ok user-details)]
       (assoc context :response response)))})
