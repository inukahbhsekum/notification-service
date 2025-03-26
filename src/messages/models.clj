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
        sender-id (UUID/fromString (:sender message-payload))
        query (-> {:insert-into   [:notification_message]
                   :columns       [:message_id :message_text
                                   :topic_id :created_by
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
                                            :created_by
                                            :created_at
                                            :updated_at]}}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (assoc message-payload :message_id message-id)
      (ur/failed message-payload))))


(defn fetch-user-pending-messages-for-topic
  [{:keys [topic_id user_id] :as zmap} {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:user_message_details]
                     :where [:and [:= :topic_id (UUID/fromString topic_id)]
                             [:= :user_id (UUID/fromString user_id)]]}
                    (sql/format {:pretty true}))
          pending-user-topic-messages (jdbc/execute-one! (db-pool)
                                                         query
                                                         {:builder-fn rs/as-unqualified-kebab-maps})]
      pending-user-topic-messages)
    (catch Exception e
      (ctl/error "User messages for topic not available")
      (throw (Exception. "User messages for topic not available")))))


(defn fetch-messages-bulk
  [message-ids {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where [:in :message_id message-ids]}
                    (sql/format {:pretty true}))
          user-messages (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
      user-messages)
    (catch Exception e
      (ctl/error "User messages not available")
      (throw (Exception. "User messages not available")))))


(defn update-message-activity-log
  [payload {:keys [db-pool]}]
  (let [id (UUID/randomUUID)
        query (-> {:insert-into   [:notification_message_activity_log]
                   :columns       [:id :message_id :topic_id :sender
                                   :receiver :meta :action_taken_at
                                   :created_at]
                   :values        [[id
                                    (:message_id payload)
                                    (:topic_id payload)
                                    (:sender_id payload)
                                    (:receiver payload)
                                    (:meta payload)
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))]]
                   :on-conflict   [:message_id
                                   {:where [:<> :message_id nil]}]
                   :do-update-set {:fields [:id
                                            :message_id
                                            :topic_id
                                            :sender
                                            :receiver
                                            :meta
                                            :action_taken_at
                                            :created_at]}}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (ur/created payload)
      (ur/failed payload))))
