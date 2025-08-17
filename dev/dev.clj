(ns dev
  (:gen-class)
  (:require [com.stuartsierra.component.repl :as component-repl]
            [config :as config]
            [core :as core]
            [websocket.server :as wss]
            [components.kafka-components :as ckc]))

(defonce service-config (config/read-config))


(component-repl/set-init
 (fn [_]
   (core/notification-service-system service-config)))


(defn -main
  []
  (component-repl/reset)
  (ckc/create-producer)
  (wss/start-websocket-server service-config))
