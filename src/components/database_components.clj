(ns components.database-components
  (:require [com.stuartsierra.component :as component]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))


(defn datasource-component
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


(defrecord DatabaseComponent
  [config]
  component/Lifecycle

  (start [component]
    (println "Starting DatabaseComponent")
    (assoc component :data-source datasource-component))

  (stop [component]
    (println "Stopping DatabaseComponent")
    (assoc component :data-source nil)))


(defn new-database-component
  [config]
  (map->DatabaseComponent config))
