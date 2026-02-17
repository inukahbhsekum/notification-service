(ns websocket.handler
  (:require [cheshire.core :as json]
            [clojure.string :as cs]
            [clojure.tools.logging :as ctl]
            [components.database-components :as cdc]
            [malli.core :as mc]
            [messages.models :as mm]
            [org.httpkit.server :as http]
            [user-details.models :as udm]
            [websocket.schema :as ws]))

(def websocket-message-validator (mc/validator ws/WebsocketMessagePayload))

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
  [_ channel {:keys [params]}]
  (let [db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        parsed-params (parse-query-params params)
        valid-message-payload (websocket-message-validator (assoc parsed-params
                                                                  :type "echo"))
        topic-id (:topic_id valid-message-payload)
        user-id (:user_id valid-message-payload)
        user-topic-details (udm/fetch-notification-topic-receiver topic-id
                                                                  user-id
                                                                  db-pool)
        user-available? (get @udm/availability-atom user-id false)
        _ (when (or (nil? user-available?) (nil? user-topic-details))
            (throw (Exception. "user_id and topic_id should be valid and online")))
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
  [_ channel {:keys [params]}]
  (let [parsed-params (parse-query-params params)
        db-pool {:db-pool (fn []
                            (cdc/new-database-pool))}
        valid-message-payload (websocket-message-validator (assoc parsed-params
                                                                  :type "connect"))
        topic-id (:topic_id valid-message-payload)
        user-id (:user_id valid-message-payload)
        user-topic-details (udm/fetch-notification-topic-receiver topic-id
                                                                  user-id
                                                                  db-pool)
        user-available? (get @udm/availability-atom user-id false)
        _ (when (or (nil? user-available?) (nil? user-topic-details))
            (throw (Exception. "user_id and topic_id should be valid and online")))
        pending-user-messages (mm/fetch-all-user-pending-messages valid-message-payload
                                                                  db-pool)
        pending-user-messages (if (nil? pending-user-messages)
                                (mm/fetch-messages-by-topic-id topic-id db-pool)
                                (->> pending-user-messages
                                     (mapv :message-id)
                                     (mm/fetch-messages-bulk db-pool)))]
    (doseq [message pending-user-messages]
      (http/send! channel (json/generate-string message)))))


(defmethod handle-websocket-message :send
  [_ channel message-payload]
  (http/send! channel (json/generate-string message-payload)))


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


(defn handle-received-message
  [{:keys [user-id message-id topic-id]} dependencies]
  (let [str-user-id (str user-id)
        user-available? (get @udm/availability-atom
                             str-user-id
                             false)]
    (if (nil? user-available?)
      (ctl/warn "User not online" {})
      (mm/upsert-user-message-details {:user_id str-user-id
                                       :message_id (str message-id)
                                       :topic_id (str topic-id)
                                       :status "read"}
                                      dependencies))))
