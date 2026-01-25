(ns consumers.notification-message-consumer
  (:require [clojure.tools.logging :as ctl]
            [components.kafka-components :as ckc]
            [constants.notification-events :as cne]
            [config :as config]
            [websocket.handler :as wh])
  (:import [java.time Duration]
           (org.apache.kafka.clients.consumer ConsumerRecord)))

(def consumer-instance nil)

(defmulti handle-event
  (fn [{:keys [medium_name event_type]}]
    [(key medium_name) (cne/event-type-event-value> event_type)]))


(defmethod handle-event [:websocket :send_message]
  [event]
  "Implementation pending")


(defmethod handle-event :default
  [_]
  (ctl/error "Invalid event-type"))


(defn create-notification-message-consumer
  [config]
  (ctl/info "Creating Notification Message Consumer..." config)
  (let [consumer (ckc/create-consumer (:message-kafka-consumer-config config))]
    (alter-var-root #'consumer-instance (constantly consumer))
    (while true
      (let [records (.poll consumer (Duration/ofMillis 1000))]
        (doseq [^ConsumerRecord record records]
          (ctl/info "consumer record ----->"
                    {"Key:" (.key record)
                     "Value:" (.value record)
                     "Partition:" (.partition record)
                     "Offset:" (.offset record)})
          (handle-event (.value record)))))
    consumer-instance))


(defn -main
  []
  (let [service-config (config/read-config)]
    (create-notification-message-consumer service-config)))
