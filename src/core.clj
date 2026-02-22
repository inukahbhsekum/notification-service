(ns core
  (:require [components.user-component :as user-component]
            [components.messages-component :as messages-component]
            [components.in-memory-state-component :as in-memory-state-component]
            [components.database-components :as db-component]
            [config :as config]
            [clojure.tools.logging :as ctl]
            [com.stuartsierra.component :as component]
            [producers.factory :as pf]
            [producers.notification-message-producer :as pnmp]))


(defn notification-service-system
  [config]
  (component/system-map
   :in-memory-state-component
   (in-memory-state-component/new-in-memory-state-component config)
   :db-pool (db-component/new-database-component config)
   :producer (pnmp/->NotificationProducer config (atom nil))
   :user-component (component/using
                    (user-component/new-user-component config)
                    [:db-pool
                     :in-memory-state-component])
   :messages-component (component/using
                        (messages-component/new-message-component config)
                        [:db-pool
                         :in-memory-state-component])))


(defn -main
  []
  (let [system (-> (config/read-config)
                   (notification-service-system)
                   (component/start-system))]
    (pf/start-producer (:producer system))
    (ctl/info "Starting notification service with config")
    (.addShutdownHook
     (Runtime/getRuntime)
     (new Thread (fn []
                   (pf/stop-producer (:producer system))
                   (component/stop-system system))))))
