(ns websocket.server
  (:require [clojure.tools.logging :as ctl]
            [org.httpkit.server :as http]
            [websocket.handler :as wh]))

(defonce server (atom nil))

(defn start-websocket-server
  [config]
  (let [port (-> config
                 :websocket-server
                 :port)]
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
