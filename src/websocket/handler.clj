(ns websocket.handler
  (:require [cheshire.core :as json]
            [org.httpkit.server :as http]
            [messages.models :as mm]
            [clojure.tools.logging :as ctl]
            [clojure.string :as cs]))


(defn- parse-query-params
  [query-string]
  (->> (cs/split query-string #"&")
       (map #(cs/split % #"="))
       (map (fn [[k v]] [(keyword k) v]))
       (into {})))


(defmulti handle-websocket-message
  "It handles the different messages recieved or sent and creates
   the databases upsert and event handling"
  (fn [_ message-payload]
    (:type message-payload)))


(defmethod handle-websocket-message :connect
  [channel {:keys [params] :as message-payload}]
  (let [{topic_id :topic_id
         user_id :user_id} (parse-query-params params)
        ;; get pending user messages 
         pending-user-messages (mm/fetch-user-messages-for-topic {:topic-id topic_id}
                                                                 )]
    (http/send! channel (json/generate-string message-payload))))


(defmethod handle-websocket-message :echo
  [channel message-payload]
  (let []
    (http/send! channel (json/generate-string message-payload))))


(defmethod handle-websocket-message :send
  [channel message-payload]
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
                             (handle-websocket-message channel data))))

        (http/on-close channel
                       (fn [status]
                         (http/send! channel
                                     (json/generate-string {:message-body (str "disconnected" status)
                                                            :type "connect"}))))))))
