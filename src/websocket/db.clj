(ns websocket.db
  (:require [config :as config]
            [components.database-components :as db-component]))


(defonce websocket-db-pool nil)


(defn setup-websocket-server-db
  []
  (let [config (config/read-config)
        db-pool (db-component/new-database-component config)]
    (alter-var-root #'websocket-db-pool db-pool)))
