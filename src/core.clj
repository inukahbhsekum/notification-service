(ns core
  (:require [components.pedestal-component :as pedestal-component]
            [components.in-memory-state-component :as in-memory-state-component]
            [components.database-components :as db-component]
            [config :as config]
            [clojure.tools.logging :as ctl]
            [com.stuartsierra.component :as component]
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


(defn notification-service-system
  [config]
  (component/system-map
    :in-memory-state-component (in-memory-state-component/new-in-memory-state-component config)
    :data-source (datasource-component config)
    :pedestal-component (component/using
                          (pedestal-component/new-pedestal-component config)
                          [:data-source
                           :in-memory-state-component])))


(defn -main
  []
  (let [system (-> (config/read-config)
                   (notification-service-system)
                   (component/start-system))]
    (ctl/info "Starting notification service with config")
    (.addShutdownHook
      (Runtime/getRuntime)
      (new Thread #(component/stop-system system)))))
