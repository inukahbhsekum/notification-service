(ns websocket.handler
  (:require [cheshire.core :as json]
            [org.httpkit.server :as http]
            [messages.models :as mm]))


(defmulti handle-websocket-message
  "It handles the different messages recieved or sent and creates
   the databases upsert and event handling"
  (fn [_ message-payload]
    (:type message-payload)))


(defmethod handle-websocket-message :connect
  [channel {:keys [params] :as message-payload}]
  (let [{topic_id :topic_id
         user_id :user_id} params
        pending-messages-for-user (mm/fetch-user-messages-for-topic {}
                                                                    ;;todo)]
    (http/send! channel
                (json/generate-string message-payload))))


(defmethod handle-websocket-message :echo
  [channel message-payload]
  (let []
    (http/send! channel message-payload)))


(defmethod handle-websocket-message :send
  [channel message-payload]
  (let []
    ()))


(defn websocket-handler
  [request]
  (let [params (:query-params request)]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
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
                           (let [received-msg (json/parse-string msg)
                                 data {:message-body (:message-body received-msg)
                                       :type :echo
                                       :params params}]
                             ;;TODO: add message to database and update linked entities
                             (handle-websocket-message channel data))))

        (http/on-close channel
                       (fn [status]
                         (http/send! channel
                                     (json/generate-string {:message-body (str "disconnected" status)
                                                            :type "connect"}))))))))