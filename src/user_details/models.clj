(ns user-details.models
  (:require [clj-time.core :as ctc]
            [clj-time.coerce :as ctco]
            [clojure.data.json :as json]
            [clojure.tools.logging :as ctl]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.convertor-utils :as ucu]
            [utils.response-utils :as ur])
  (:import (java.util UUID)))


(defn create-or-update-user
  [user-payload {:keys [db-pool]}]
  (let [user-id (:user_id user-payload)
        query (-> {:insert-into   [:notification_user]
                   :columns       [:user_id :user_first_name
                                   :user_middle_name :user_last_name
                                   :user_type :user_metadata
                                   :created_at :updated_at]
                   :values        [[user-id
                                    (:first_name user-payload)
                                    (:middle_name user-payload)
                                    (:last_name user-payload)
                                    [:cast (:user_type user-payload) :user_types]
                                    (ucu/sql-json-> (:user_metadata user-payload))
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))]]
                   :on-conflict   [:user_id
                                   {:where [:<> :user_id nil]}]
                   :do-update-set {:fields [:user_first_name
                                            :user_middle_name
                                            :user_last_name
                                            :user_type
                                            :user_metadata
                                            :updated_at]}}
                  (sql/format {:pretty true}))
        user-details (jdbc/execute-one! (db-pool)
                                        query
                                        {:builder-fn rs/as-unqualified-kebab-maps})]
    (if user-details
      (ur/created (str user-id))
      (ur/failed (str user-id)))))


(defn fetch-user-details
  [user-id {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from   [:notification_user]
                     :where  [:= :user_id (UUID/fromString user-id)]}
                    (sql/format {:pretty true}))
          user-details (jdbc/execute-one! (db-pool)
                                          query
                                          {:builder-fn rs/as-unqualified-kebab-maps})]
      (assoc user-details
             :user-metadata (-> :user-metadata
                                user-details
                                .getValue
                                json/read-json)
             :user-id (str (:user-id user-details))))
    (catch Exception e
      (ctl/error "User not found " (ex-message e))
      (throw (Exception. "Invalid user_id")))))


(defn create-or-update-topic
  [topic-payload {:keys [db-pool]}]
  (let [topic-id (:topic_id topic-payload)
        query (-> {:insert-into   [:notification_topic]
                   :columns       [:topic_id
                                   :title
                                   :description
                                   :created_at
                                   :updated_at]
                   :values        [[topic-id
                                    (:title topic-payload)
                                    (:description topic-payload)
                                    (ctco/to-sql-time (ctc/now))
                                    (ctco/to-sql-time (ctc/now))]]
                   :on-conflict   [:topic_id
                                   {:where [:<> :topic_id nil]}]
                   :do-update-set {:fields [:title
                                            :description
                                            :updated_at]}}
                  (sql/format {:pretty true}))
        topic-details (jdbc/execute-one! (db-pool)
                                         query
                                         {:builder-fn rs/as-unqualified-kebab-maps})]
    (if topic-details
      (ur/created (str topic-id))
      (ur/failed (str topic-id)))))


(defn- update-user-topic
  [user-id topic-id db-pool]
  (let [query (-> {:insert-into [:user_notification_topic]
                   :columns     [:user_id
                                 :topic_id]
                   :values      [[(UUID/fromString user-id)
                                  (UUID/fromString topic-id)]]}
                  (sql/format {:pretty true}))
        user-topic-mapping (jdbc/execute-one! (db-pool)
                                              query
                                              {:builder-fn rs/as-unqualified-kebab-maps})]
    user-topic-mapping))


(defn update-notification-receivers
  [{:keys [user-ids topic-id]} {:keys [db-pool]}]
  (let [update-user-topic (fn [response-map user-id]
                            (try
                              (update-user-topic user-id topic-id db-pool)
                              (update-in response-map [:success] conj user-id)
                              (catch Exception e
                                (update-in response-map [:failure] conj user-id)
                                (update-in response-map [:exceptions] conj e))))
        update-response-map (reduce update-user-topic
                                    {:success []
                                     :failure []
                                     :exceptions []}
                                    user-ids)]
    (when (seq (:failure update-response-map))
      (ctl/error "Users can't be mapped to topic" (apply str (mapv #(ex-message %) (:exceptions update-response-map))))
      (ur/failed "Users can't be mapped to topic"))))


(defn fetch-notification-topic-receivers
  [topic-id {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from   [:user_notification_topic]
                     :where  [:= :topic_id (UUID/fromString topic-id)]}
                    (sql/format {:pretty true}))
          notification-topic-receivers (jdbc/execute! (db-pool)
                                                      query
                                                      {:builder-fn rs/as-unqualified-kebab-maps})]
      notification-topic-receivers)
    (catch Exception e
      (ctl/error "User not found " (ex-message e))
      (ur/not-found (str "topic receivers not found with id: " topic-id)))))


(defn fetch-notification-topic-receiver
  [topic-id receiver-id {:keys [db-pool]}]
  (try
    (let [query (-> {:select [:*]
                     :from   [:user_notification_topic]
                     :where  [:and
                              [:= :topic_id (UUID/fromString topic-id)]
                              [:= :user_id (UUID/fromString receiver-id)]]}
                    (sql/format {:pretty true}))
          notification-topic-receiver (jdbc/execute-one! (db-pool)
                                                         query
                                                         {:builder-fn rs/as-unqualified-kebab-maps})]
      notification-topic-receiver)
    (catch Exception e
      (ctl/error "Reciever topic mapping does not exist" (ex-message e))
      (ur/not-found (str "Reciever topic mapping does not exist" topic-id receiver-id)))))
