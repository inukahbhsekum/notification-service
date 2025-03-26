(ns websocket.handler
  (:require [cheshire.core :as json]
            [clojure.string :as cs]
            [messages.models :as mm]
            [org.httpkit.server :as http]
            [schema.core :as sc]
            [websocket.server :as wser]
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
  (let [websocket-db-pool wser/websocket-db-pool
        parsed-params (parse-query-params params)
        valid-message-payload (sc/validate ws/WebsocketMessagePayload
                                           (assoc parsed-params
                                                  :type "connect"))
        pending-user-messages (mm/fetch-user-pending-messages-for-topic valid-message-payload
                                                                        {:db-pool websocket-db-pool})
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
    (http/as-channel request
                     {:on-open (fn [channel]
                                 (handle-websocket-message channel {:message-body "connection established"
                                                                    :type :connect
                                                                    :params params}))

                      :on-message (fn [channel msg]
                                    (handle-websocket-message channel {:message-body msg
                                                                       :type :echo
                                                                       :params params}))

                      :on-close (fn [channel status]
                                  (http/send! channel
                                              (json/generate-string {:message-body (str "disconnected" status)
                                                                     :type "connect"})))

                      :on-error (fn [channel error]
                                  (http/send! channel
                                              (json/generate-string {:message-body (str "disconnected" error)
                                                                     :type "connect"})))})))
