(ns core
  (:require [components.pedestal-component :as pedestal-component]
            [components.in-memory-state-component :as in-memory-state-component]
            [components.database-components :as db-component]
            [config :as config]
            [clojure.tools.logging :as ctl]
            [com.stuartsierra.component :as component]))


(defn notification-service-system
  [config]
  (component/system-map
    :in-memory-state-component (in-memory-state-component/new-in-memory-state-component config)
    :data-source (db-component/new-database-component config)
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
