(ns user-details.handlers
  (:require [utils.response-utils :as ur]
            [user-details.validation :as udv]
            [user-details.models :as udm]))


(defn- create-user
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        udv/validate-user-creation-request
        (udm/create-or-update-user dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def register-user
  {:name :register-user-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [user-details (create-user request dependencies)
           response (ur/ok user-details)]
       (assoc context :response response)))})


(def get-user
  {:name :fetch-user-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [params (:query-params request)
           user-id (:user_id params)
           user-details (udm/fetch-user-details user-id dependencies)]
       (assoc context :response user-details)))})


(defn- create-the-topic
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        (udv/validate-topic-creation-request dependencies)
        (udm/create-or-update-topic dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def create-topic
  {:name :create-topic-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [topic-details (create-the-topic request dependencies)
           response (ur/ok topic-details)]
       (assoc context :response response)))})


(defn- create-the-topic-user-mapping
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        (udv/validate-topic-user-mapping-request dependencies)
        (udm/update-notification-receivers dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def create-topic-user-mapping
  {:name :create-topic-user-mapping
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [topic-user-mapping (create-the-topic-user-mapping request
                                                             dependencies)
           response (ur/ok topic-user-mapping)]
       (assoc context :response response)))})
