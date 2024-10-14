(ns message.models
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


(defn- update-user-topic
  [user-id topic-id db-pool]
  (let [query (-> {:insert-into [:user_notification_topic]
                   :columns     [:user_id
                                 :topic_id]
                   :values      [[user-id
                                  topic-id]]}
                  (sql/format {:pretty true}))
        user-topic-mapping (jdbc/execute-one! (db-pool)
                                              query
                                              {:builder-fn rs/as-unqualified-kebab-maps})]
    user-topic-mapping))


(defn update-notification-receivers
  [user-ids topic-id {:keys [db-pool]}]
  (try
    (map (fn [user-id]
           (update-user-topic user-id topic-id db-pool))
         user-ids)
    (catch Exception e
      (ctl/error "Users can't be mapped to topic" (ex-message e))
      (ur/failed "Users can't be mapped to topic"))))
