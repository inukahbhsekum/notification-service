(ns dev
  (:gen-class)
  (:require [com.stuartsierra.component.repl :as component-repl]
            [config :as config]
            [core :as core]))


(component-repl/set-init
 (fn [_]
   (core/notification-service-system (config/read-config))))


(defn -main
  []
  (component-repl/reset))
