(ns websocket.db
  (:require [config :as config]
            [components.database-components :as db-component]))


(def websocket-db-pool (atom nil))


(defn setup-websocket-server-db
  []
  (let [config (config/read-config)
        db-pool (db-component/new-database-component config)]
    (reset! websocket-db-pool db-pool)))
