(ns websocket.handler
  (:require [cheshire.core :as json]
            [clojure.string :as cs]
            [messages.models :as mm]
            [org.httpkit.server :as http]
            [schema.core :as sc]
            [websocket.schema :as ws]))


(defn- parse-query-params
  [query-string]
  (->> (cs/split query-string #"&")
       (map #(cs/split % #"="))
       (map (fn [[k v]] [(keyword k) v]))
       (into {})))


(defmulti handle-websocket-message
  "It handles the different messages recieved or sent and creates
   the databases upsert and event handling"
  (fn [_ _ message-payload]
    (:type message-payload)))


(defmethod handle-websocket-message :connect
  [request channel {:keys [params]}]
  (let [{topic_id :topic_id
         user_id :user_id} (parse-query-params params)
        valid-message-payload (sc/validate ws/WebsocketMessagePayload {:topic_id topic_id
                                                                       :user_id user_id
                                                                       :type :connect})
        pending-user-messages (mm/fetch-user-pending-messages-for-topic {:topic-id topic_id
                                                                         :user-id user_id}
                                                                        request)
        pending-message-ids (mapv :message_id pending-user-messages)
        pending-messages (mm/fetch-messages-bulk pending-message-ids request)]
    (doseq [message pending-messages]
      (http/send! channel (json/generate-string {:message-body (:message_text message)
                                                 :type :connect
                                                 :params params})))))


(defmethod handle-websocket-message :echo
  [request channel message-payload]
  (let []
    (http/send! channel (json/generate-string message-payload))))


(defmethod handle-websocket-message :send
  [request channel message-payload]
  (let []
    (http/send! channel (json/generate-string message-payload))))


(defn websocket-handler
  [request]
  (let [params (:query-string request)]
    (http/with-channel request channel
      (do
        ;; send a connection established message
        ;; this message is not stored in the db
        ;; this is to just make sure that connection is set
        ;; between client and server
        (handle-websocket-message channel
                                  {:message-body "connection established"
                                   :type :connect
                                   :params params})

        (http/on-receive channel
                         (fn [msg]
                           (let [data {:message-body msg
                                       :type :echo
                                       :params params}]
                             ;;TODO: add message to database and update linked entities
                             (handle-websocket-message request channel data))))

        (http/on-close channel
                       (fn [status]
                         (http/send! channel
                                     (json/generate-string {:message-body (str "disconnected" status)
                                                            :type "connect"}))))))))
