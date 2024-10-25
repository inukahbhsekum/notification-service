(ns components.messages-component
  (:require [clojure.tools.logging :as ctl]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [messages.routes :as routes]))


(defn inject-dependencies
  [dependencies]
  (interceptor/interceptor
    {:name  ::inject-dependencies
     :enter (fn [context]
              (assoc context :dependencies dependencies))}))


(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["application/json"]))


(defrecord MessageComponent
  [config db-pool in-memory-state-component]
  component/Lifecycle

  (start [component]
    (ctl/info "Starting MessageComponent")
    (let [server (-> {::http/routes routes/routes
                      ::http/type   :jetty
                      ::http/join?  false
                      ::http/port   (-> config
                                        :server
                                        :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat
                             [(inject-dependencies component)
                              content-negotiation-interceptor])
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (ctl/info "Stopping MessageComponent")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))


(defn new-message-component
  [config]
  (map->MessageComponent {:config config}))
