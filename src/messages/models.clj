(ns messages.models
  (:require [clj-time.core :as ctc]
            [clj-time.coerce :as ctco]
            [clojure.test :refer :all]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.response-utils :as ur]
            [clojure.tools.logging :as ctl])
  (:import (java.util UUID)))


(defn create-or-update-message
  [message-payload {:keys [db-pool]}]
  (let [message-id (UUID/randomUUID)
        topic-id (UUID/fromString (:topic_id message-payload))
        sender-id (UUID/fromString (:manager_id message-payload))
        query (-> {:insert-into   [:notification_message]
                   :columns       [:message_id :message_text
                                   :topic_id :sender
                                   :created_at :updated_at]
                   :values        [[message-id
                                    (:message_text message-payload)
                                    topic-id
                                    sender-id
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))]]
                   :on-conflict   [:message_id
                                   {:where [:<> :message_id nil]}]
                   :do-update-set {:fields [:message_id
                                            :message_text
                                            :topic_id
                                            :sender
                                            :created_at
                                            :updated_at]}}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (ur/created (assoc message-payload :message_id message-id))
      (ur/failed message-payload))))


(defn fetch-user-messages-for-topic
  [{:keys [topic-id] :as zmap} {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where [:= :topic_id (UUID/fromString topic-id)]}
                    (sql/format {:pretty true}))
          topic-messages (jdbc/execute-one! (db-pool)
                                            query
                                            {:builder-fn rs/as-unqualified-kebab-maps})]
      topic-messages)
    (catch Exception e
      (ctl/error "User not found " (ex-message e))
      (throw (Exception. "Invalid user-id")))))


(defn fetch-user-pending-messages-for-topic
  [{:keys [topic-id user-id] :as zmap} {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where
                     [:= :topic_id (UUID/fromString topic-id)]
                     [:= :user_id (UUID/fromString user-id)]}
                    (sql/format {:pretty true}))
          topic-messages (jdbc/execute-one! (db-pool)
                                            query
                                            {:builder-fn rs/as-unqualified-kebab-maps})]
      (catch Exception e
        (ctl/error "User messages for topic not available")
        (throw (Exception. "User messages for topic not available"))))))
