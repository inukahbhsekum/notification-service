(ns messages.models
  (:require [clj-time.core :as ctc]
            [clj-time.coerce :as ctco]
            [clojure.test :refer :all]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.response-utils :as ur])
  (:import (java.util UUID)))


(defn create-or-update-message
  [message-payload {:keys [db-pool]}]
  (let [message-id (UUID/randomUUID)
        topic-id (UUID/fromString (:topic_id message-payload))
        sender-id (UUID/fromString (:sender message-payload))
        receiver (UUID/fromString (:receiver message-payload))
        query (-> {:insert-into   [:notification_message]
                   :columns       [:message_id :message_text
                                   :topic_id :sender
                                   :receiver :created_at
                                   :updated_at]
                   :values        [[message-id
                                    (:message_text message-payload)
                                    topic-id
                                    sender-id
                                    receiver
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))]]
                   :on-conflict   [:message_id
                                   {:where [:<> :message_id nil]}]
                   :do-update-set {:fields [:message_id
                                            :message_text
                                            :topic_id
                                            :sender
                                            :receiver
                                            :created_at
                                            :updated_at]}}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (ur/created (assoc message-payload :message_id message-id))
      (ur/failed message-payload))))


(defn create-message-activity-log
  [{:keys [sender receiver topic_id message_id message_text action_taken_at]}
   {:keys [db-pool]}]
  (let [query (-> {:insert-into   [:notification_message_activity_log]
                   :columns       [:message-id :topic_id
                                   :sender :receiver
                                   :action_taken_at
                                   :created_at]
                   :values        [[message_id
                                    message_text
                                    topic_id
                                    sender
                                    receiver
                                    action_taken_at
                                    (ctco/to-sql-time (ctc/now))]]
                   :on-conflict   [:message_id
                                   {:where [:<> :message_id nil]}]
                   :do-update-set {:fields [:message_id
                                            :message_text
                                            :topic_id
                                            :sender
                                            :receiver
                                            :created_at]}}
                  (sql/format {:pretty true}))
        message-log-details (jdbc/execute-one! (db-pool)
                                               query
                                               {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-log-details
      (ur/created message_id)
      (ur/failed message_id))))
