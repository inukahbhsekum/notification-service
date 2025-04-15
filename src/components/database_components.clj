(ns components.database-components
  (:require [config :as config]
            [next.jdbc :as nj]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))

(def datasource (atom nil))

(defn- get-database-pool-component
  [config]
  (let [init-function (fn [datasource]
                        (.migrate
                         (.. (Flyway/configure)
                             (dataSource datasource)
                             (locations (into-array String ["classpath:database/migrations"]))
                             (table "schema_version")
                             (load))))]
    (connection/component HikariDataSource
                          (assoc (:db-spec config) :init-fn init-function))))


(defn new-database-component
  [config]
  (get-database-pool-component config))


(defn new-database-pool
  []
  (if @datasource
    @datasource
    (let [db-pool (nj/get-datasource (:db-spec (config/read-config)))]
      (reset! datasource db-pool)
      db-pool)))
