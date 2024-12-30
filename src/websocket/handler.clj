(ns websocket.handler
  (:require [cheshire.core :as json]
            [org.httpkit.server :as http]))


(defn websocket-handler
  [request]
  (let [params (:query-params request)
        topic-id (get params "topic_id")]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (http/with-channel request channel
      (do
        ;; send a connection established message
        ;; this message is not stored in the db
        ;; this is to just make sure that connection is set
        ;; between client and server
        (http/send! channel
                    (json/generate-string {:message-body "connection established"
                                           :type "connect"}))

        (http/on-receive channel
                         (fn [msg]
                           (let [received-msg (json/parse-string msg)
                                 data {:message-body (:message-body received-msg)
                                       :type "echo"}]
                             ;;TODO: add message to database and update linked entities
                             (http/send! channel
                                         (json/generate-string data)))))

        (http/on-close channel
                       (fn [status]
                         (http/send! channel
                                     (json/generate-string {:message-body (str "disconnected" status)
                                                            :type "connect"}))))))))
