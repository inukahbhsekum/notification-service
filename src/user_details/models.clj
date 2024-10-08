(ns user-details.models
  (:require [clj-time.core :as ctc]
            [clj-time.coerce :as ctco]
            [cheshire.core :refer [generate-string]]
            [clojure.test :refer :all]
            [clojure.tools.logging :as ctl]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.response-utils :as ur])
  (:import [java.util UUID]))


(defn create-user
  [user-payload {:keys [db-pool]}]
  (let [user-id (UUID/randomUUID)
        query (-> {:insert-into [:notification_user]
                   :columns     [:user_id :user_first_name
                                 :user_middle_name :user_last_name
                                 :user_type :user_metadata
                                 :created_at :updated_at]
                   :values      [[user-id
                                  (:first_name user-payload)
                                  (:middle_name user-payload)
                                  (:last_name user-payload)
                                  (:user_type user-payload)
                                  (generate-string (:user_metadata user-payload)
                                                   {:pretty true})
                                  (ctco/to-sql-time (ctc/now))
                                  (ctco/to-sql-time (ctc/now))]]}
                  (sql/format {:pretty true}))
        user-details (jdbc/execute-one! (db-pool)
                                        query
                                        {:builder-fn rs/as-unqualified-kebab-maps})]
    (if user-details
      (ur/created (:user_id user-details))
      (ur/failed))))
