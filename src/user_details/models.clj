(ns user-details.models
  (:require [clj-time.core :as ctc]
            [clj-time.coerce :as ctco]
            [clojure.data.json :as json]
            [clojure.test :refer :all]
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
                                    (:user_type user-payload)
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
                           json/read-json)))
    (catch Exception e
      (ctl/error "User not found " (ex-message e))
      (ur/not-found (str "User not found with id: " user-id)))))


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
