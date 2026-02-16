(ns messages.handler
  (:require [utils.response-utils :as ur]
            [messages.core :as mc]
            [messages.validation :as mv]
            [messages.models :as mm]
            [clojure.tools.logging :as ctl]))


(defn- create-message
  [request dependencies]
  (try
    (let [response (-> {:request-body (:json-params request)
                        :params       (:query-params request)}
                       (mv/validate-message-creation-request dependencies)
                       (mm/create-or-update-message dependencies))]
      (future
        (try
          (mc/update-message-activity-log response dependencies)
          (mc/update-user-message-details response dependencies)
          (ctl/log :info "Future execution finished successfully")
          (catch Exception e
            (ctl/log :error (str "Error in future execution:" (.getMessage e))))))
      (ur/created response))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def create-message-handler
  {:name :create-message-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [message (create-message request dependencies)
           response (ur/ok message)]
       (assoc context :response response)))})


(defn- add-message-event-type
  [payload _]
  (assoc-in payload [:message_details :event-type] 0))


(defn- send-message
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params       (:query-params request)}
        (mv/validate-send-message-request dependencies)
        (add-message-event-type dependencies)
        (mc/send-message dependencies)
        (future
          (mc/update-message-activity-log dependencies)))
    (ur/ok "Message sent successfully")
    (catch Exception e
      (ur/failed (ex-message e)))))


(def send-message-handler
  {:name :send-message-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [message (send-message request dependencies)
           response (ur/ok message)]
       (assoc context :response response)))})


(defn- fetch-messages
  [request dependencies]
  (try
    (-> {:request-body (:json-params request)
         :params (:query-params request)}
        (mv/validate-fetch-message-request dependencies)
        (mm/fetch-topic-messages-paginated dependencies))
    (catch Exception e
      (ur/failed (ex-message e)))))


(def fetch-message-handler
  {:name :fetch-message-handler
   :enter
   (fn [{:keys [request dependencies] :as context}]
     (let [messages (fetch-messages request dependencies)
           response (ur/ok messages)]
       (assoc context :response response)))})
