(ns websocket.server
  (:require [clojure.tools.logging :as ctl]
            [components.database-components :as db-component]
            [config :as config]
            [org.httpkit.server :as http]
            [websocket.handler :as wh]))

(defonce server (atom nil))
(defonce websocket-db-pool nil)

(defn setup-websocket-server
  []
  (let [config (config/read-config)
        db-pool (db-component/new-database-component config)]
    (alter-var-root #'websocket-db-pool db-pool)))


(defn start-websocket-server
  [config]
  (let [port (-> config
                 :websocket-server
                 :port)]
    (setup-websocket-server)
    (->> {:port port}
         (http/run-server wh/websocket-handler)
         (reset! server))
    (ctl/info (str "WebSocket server running on ws://localhost:" port))))


(defn stop-websocket-server
  []
  (when @server
    (@server)
    (reset! server nil)
    (ctl/info "WebSocket server stopped")))
