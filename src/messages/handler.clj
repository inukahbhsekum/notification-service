(ns messages.handler
  (:require [utils.response-utils :as ur]
            [messages.core :as mc]
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
     (let [message (create-message request dependencies)
           response (ur/ok message)]
       (assoc context :response response)))})


(defn- send-message
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        (mv/validate-send-message-request dependencies)
        (mc/send-message dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def send-message-handler
  {:name :send-message-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [message (send-message request dependencies)
           response (ur/ok message)]
       (assoc context :response response)))})
