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
    (let [server-map (-> {::http/routes routes/routes
                          ::http/type   :jetty
                          ::http/join?  false
                          ::http/port   (-> config
                                            :notification-server
                                            :port)}
                         (http/default-interceptors)
                         (update ::http/interceptors concat
                                 [(inject-dependencies component)
                                  content-negotiation-interceptor]))
          server (:server component)]
      (when (not-empty server)
        (ctl/info "Stopping existing UserComponent")
        (http/stop server))
      (ctl/info "Starting New UserComponent")
      (assoc component :server (-> server-map
                                   (http/create-server)
                                   (http/start)))))

  (stop [component]
    (when-let [server (:server component)]
      (ctl/info "Stopping UserComponent")
      (http/stop server))
    (assoc component :server nil)))


(defn new-user-component
  [config]
  (map->UserComponent {:config config}))
