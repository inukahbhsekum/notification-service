(ns producers.notification-message-producer
  (:require [components.kafka-components :as ckc]
            [config :as config]))

(def message-producer nil)

(defn create-notification-message-producer
  [config]
  (let [producer (ckc/create-producer (:message-kafka-producer-config config))]
    (alter-var-root #'message-producer (constantly producer))))


(defn -main
  []
  (let [service-config (config/read-config)]
    (create-notification-message-producer service-config)))
