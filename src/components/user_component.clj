(ns components.user-component
  (:require [clojure.tools.logging :as ctl]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [user-details.routes :as routes]))


(defn inject-dependencies
  [dependencies]
  (interceptor/interceptor
    {:name  ::inject-dependencies
     :enter (fn [context]
              (assoc context :dependencies dependencies))}))


(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["application/json"]))


(defrecord UserComponent
  [config db-pool in-memory-state-component]
  component/Lifecycle

  (start [component]
    (ctl/info "Starting UserComponent")
    (let [server (-> {::http/routes routes/routes
                      ::http/type   :jetty
                      ::http/join?  false
                      ::http/port   (-> config
                                        :notification-server
                                        :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat
                             [(inject-dependencies component)
                              content-negotiation-interceptor])
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (ctl/info "Stopping UserComponent")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))


(defn new-user-component
  [config]
  (map->UserComponent {:config config}))
