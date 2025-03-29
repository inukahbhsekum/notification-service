(ns websocket.server
  (:require [clojure.tools.logging :as ctl]
            [config :as config]
            [org.httpkit.server :as http]
            [websocket.db :as wd]
            [websocket.handler :as wh]))

(defonce server (atom nil))


(defn start-websocket-server
  [config]
  (let [port (-> config
                 :websocket-server
                 :port)]
    (wd/setup-websocket-server-db)
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
