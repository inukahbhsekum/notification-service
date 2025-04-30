(ns websocket.handler
  (:require [cheshire.core :as json]
            [clojure.string :as cs]
            [components.database-components :as cdc]
            [messages.models :as mm]
            [org.httpkit.server :as http]
            [schema.core :as sc]
            [user-details.models :as udm]
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


(defmethod handle-websocket-message :echo
  [request channel {:keys [params]}]
  (let [db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        parsed-params (parse-query-params params)
        valid-message-payload (sc/validate ws/WebsocketMessagePayload
                                           (assoc parsed-params
                                                  :type "echo"))
        topic-id (:topic_id valid-message-payload)
        user-topic-details (udm/fetch-notification-topic-receiver topic-id
                                                                  (:user_id valid-message-payload)
                                                                  db-pool)
        _ (when (nil? user-topic-details)
            (throw (Exception. "user_id and topic_id should be valid")))
        pending-user-messages (mm/fetch-user-pending-messages-for-topic valid-message-payload
                                                                        db-pool)
        pending-user-messages (if (nil? pending-user-messages)
                                (mm/fetch-messages-by-topic-id topic-id db-pool)
                                (->> pending-user-messages
                                     (mapv :message-id)
                                     (mm/fetch-messages-bulk db-pool)))]
    (doseq [message pending-user-messages]
      (http/send! channel (json/generate-string {:message-body (:message-text message)
                                                 :type :echo
                                                 :params params})))))


(defmethod handle-websocket-message :connect
  [request channel {:keys [params]}]
  (let [parsed-params (parse-query-params params)
        db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        valid-message-payload (sc/validate ws/WebsocketMessagePayload
                                           (assoc parsed-params
                                                  :type "connect"))
        topic-id (:topic_id valid-message-payload)
        user-topic-details (udm/fetch-notification-topic-receiver topic-id
                                                                  (:user_id valid-message-payload)
                                                                  db-pool)
        _ (when (nil? user-topic-details)
            (throw (Exception. "user_id and topic_id should be valid")))
        pending-user-messages (mm/fetch-user-pending-messages valid-message-payload
                                                              db-pool)
        pending-user-messages (if (nil? pending-user-messages)
                                (mm/fetch-messages-by-topic-id topic-id db-pool)
                                (->> pending-user-messages
                                     (mapv :message-id)
                                     (mm/fetch-messages-bulk db-pool)))]
    (doseq [message pending-user-messages]
      (http/send! channel (json/generate-string message)))))


(defmethod handle-websocket-message :send
  [request channel message-payload]
  (let []
    (http/send! channel (json/generate-string message-payload))))


(defn websocket-handler
  [request]
  (let [params (:query-string request)]
    (http/as-channel request
                     {:on-open (fn [channel]
                                 (handle-websocket-message request
                                                           channel
                                                           {:message-body "connection established"
                                                            :type :connect
                                                            :params params}))

                      :on-message (fn [channel msg]
                                    (handle-websocket-message request
                                                              channel
                                                              {:message-body msg
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
