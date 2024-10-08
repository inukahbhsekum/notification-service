(ns components.database-components
  (:require [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))


(defn- get-database-pool
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
  (get-database-pool config))
