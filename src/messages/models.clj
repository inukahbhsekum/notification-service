(ns messages.models
  (:require [clj-time.coerce :as ctco]
            [clj-time.core :as ctc]
            [clojure.tools.logging :as ctl]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.convertor-utils :refer [sql-json->]]
            [utils.response-utils :as ur])
  (:import (java.util UUID)))


(defn create-or-update-message
  [message-payload {:keys [db-pool]}]
  (let [message-id (UUID/randomUUID)
        topic-id (UUID/fromString (:topic_id message-payload))
        sender-id (UUID/fromString (:sender message-payload))
        query (-> {:insert-into   [:notification_message]
                   :columns       [:message_id :message_text
                                   :topic_id :created_by
                                   :created_at :updated_at
                                   :message_medium]
                   :values        [[message-id
                                    (:message_text message-payload)
                                    topic-id
                                    sender-id
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))
                                    (:message_medium message-payload)]]
                   :on-conflict   [:message_id
                                   {:where [:<> :message_id nil]}]
                   :do-update-set {:fields [:message_id
                                            :message_text
                                            :topic_id
                                            :created_by
                                            :created_at
                                            :updated_at
                                            :message_medium]}}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (assoc message-payload :message_id message-id)
      (ur/failed message-payload))))


(defn fetch-message-by-message-id
  [message_id {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where [:= :message_id (UUID/fromString message_id)]}
                    (sql/format {:pretty true}))
          message-details (jdbc/execute-one! (db-pool)
                                             query
                                             {:builder-fn rs/as-unqualified-kebab-maps})]
      message-details)
    (catch Exception e
      (ctl/error "Message not found with the message-id" e)
      (throw (Exception. "Message not found with message-id")))))


(defn fetch-message-medium
  [message_medium {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_medium]
                     :where [:= :medium_name message_medium]}
                    (sql/format {:pretty true}))
          medium-details (jdbc/execute-one! (db-pool)
                                            query
                                            {:builder-fn rs/as-unqualified-kebab-maps})]
      medium-details)
    (catch Exception e
      (ctl/error (str "Message medium not found with the medium " message_medium) e)
      (throw (Exception. (str "Message medium not found with the medium " message_medium))))))


(defn fetch-message-medium-by-id
  [medium_id {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_medium]
                     :where [:= :id medium_id]}
                    (sql/format {:pretty true}))
          medium-details (jdbc/execute-one! (db-pool)
                                            query
                                            {:builder-fn rs/as-unqualified-kebab-maps})]
      medium-details)
    (catch Exception e
      (ctl/error (str "Message medium not found with the mediumID " medium_id) e)
      (throw (Exception. (str "Message medium not found with the mediumID " medium_id))))))


(defn fetch-user-pending-messages-for-topic
  [{:keys [topic_id user_id]} {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:user_message_details]
                     :where [:and [:= :topic_id (UUID/fromString topic_id)]
                             [:= :user_id (UUID/fromString user_id)]]}
                    (sql/format {:pretty true}))
          pending-user-topic-messages (jdbc/execute! (db-pool)
                                                     query
                                                     {:builder-fn rs/as-unqualified-kebab-maps})]
      pending-user-topic-messages)
    (catch Exception _
      (ctl/error "User messages for topic not available")
      (throw (Exception. "User messages for topic not available")))))


(defn fetch-all-user-pending-messages
  [{:keys [user_id]} {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:user_message_details]
                     :where [:= :user_id (UUID/fromString user_id)]}
                    (sql/format {:pretty true}))
          pending-user-topic-messages (jdbc/execute! (db-pool)
                                                     query
                                                     {:builder-fn rs/as-unqualified-kebab-maps})]
      pending-user-topic-messages)
    (catch Exception _
      (ctl/error "User messages for user_id not available")
      (throw (Exception. "User messages for user_id not available")))))


(defn fetch-topic-messages-paginated
  [{:keys [topic_id from limit]
    :or {from (ctc/epoch)
         limit 10}}
   {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where [:and [:> :created_at (ctco/to-sql-time from)]
                             [:= :topic_id topic_id]]
                     :order-by [[:created_at :asc]]
                     :limit limit}
                    (sql/format {:pretty true}))
          pending-topic-messages (jdbc/execute! (db-pool)
                                                query
                                                {:builder-fn rs/as-unqualified-kebab-maps})]
      pending-topic-messages)
    (catch Exception _
      (ctl/error "User messages for user_id in the given timerange not available")
      (throw (Exception. "User messages for user_id in the given timerange not available")))))


(defn fetch-messages-bulk
  [{:keys [db-pool]} message-ids]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where [:in :message_id message-ids]}
                    (sql/format {:pretty true}))
          user-messages (jdbc/execute! (db-pool)
                                       query
                                       {:builder-fn rs/as-unqualified-kebab-maps})]
      user-messages)
    (catch Exception _
      (ctl/error "User messages not available")
      (throw (Exception. "User messages not available")))))


(defn fetch-messages-by-topic-id
  [topic-id {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from [:notification_message]
                     :where [:in :topic_id topic-id]}
                    (sql/format {:pretty true}))
          user-messages (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
      user-messages)
    (catch Exception _
      (ctl/error "User messages not available")
      (throw (Exception. "User messages not available")))))


(defn update-message-activity-log
  [payload {:keys [db-pool]}]
  (let [id (UUID/randomUUID)
        query (-> {:insert-into   [:notification_message_activity_log]
                   :columns       [:id :message_id :topic_id :sender
                                   :receivers :meta :action_taken_at
                                   :created_at :message_medium]
                   :values        [[id
                                    (:message_id payload)
                                    (:topic_id payload)
                                    (:sender payload)
                                    (sql/call :array (:receivers payload))
                                    (sql-json-> (:meta payload))
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))
                                    (:message_medium payload)]]
                   :on-conflict   [:id
                                   {:where [:<> :id nil]}]
                   :do-update-set {:fields [:id
                                            :message_id
                                            :topic_id
                                            :sender
                                            :receivers
                                            :meta
                                            :action_taken_at
                                            :created_at
                                            :message_medium]}}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (ur/created payload)
      (ur/failed payload))))


(defn upsert-user-message-details
  [payload {:keys [db-pool]}]
  (let [query (-> {:insert-into   [:user_message_details]
                   :columns       [:user_id :message_id :topic_id :status]
                   :values        [[(:user_id payload)
                                    (:message_id payload)
                                    (UUID/fromString (:topic_id payload))
                                    [:cast (:status payload) :message_status]]]}
                  (sql/format {:pretty true}))
        message-details (jdbc/execute-one! (db-pool)
                                           query
                                           {:builder-fn rs/as-unqualified-kebab-maps})]
    (if message-details
      (ur/created payload)
      (ur/failed payload))))
