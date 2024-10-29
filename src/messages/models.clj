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
        query (-> {:insert-into   [:notification_message]
                   :columns       [:message-id :message_text
                                   :topic_id :sender
                                   :receiver :created_at
                                   :updated_at]
                   :values        [[message-id
                                    (:message_text message-payload)
                                    (:topic_id message-payload)
                                    (:sender message-payload)
                                    (:receiver message-payload)
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
        user-details (jdbc/execute-one! (db-pool)
                                        query
                                        {:builder-fn rs/as-unqualified-kebab-maps})]
    (if user-details
      (ur/created (str message-id))
      (ur/failed (str message-id)))))


(defn update-message-activity-log
  [message request]
  ()
  ;; in progress
  )