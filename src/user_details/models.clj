(ns user-details.models
  (:require [clj-time.core :as ctc]
            [clojure.test :refer :all]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.response-utils :as ur])
  (:import [java.util UUID]))


(defn create-user
  [user-payload {:keys [data-source]}]
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
                                  (:user_metadata user-payload)
                                  (ctc/now)
                                  (ctc/now)]]}
                  (sql/format {:pretty true}))
        user-details (jdbc/execute-one! (data-source)
                                        query
                                        {:builder-fn rs/as-unqualified-kebab-maps})]
    (if user-details
      (ur/created (:user_id user-details))
      (ur/failed))))
