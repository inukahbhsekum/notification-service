(ns producers.notification-message-producer
  (:require [clojure.tools.logging :as ctl]
            [com.stuartsierra.component :as component])
  (:import (java.util Properties)
           (org.apache.kafka.clients.producer Callback KafkaProducer ProducerRecord)))

(defprotocol KafkaProducerProtocol
  (send-event [this topic key value] "send an event to a producer"))


(defrecord NotificationProducer [config]
  component/Lifecycle
  (start
    [component]
    (let [existing-producer (:producer component)]
      (when (not-empty existing-producer)
        (ctl/info "Closing existing producer")
        (.close existing-producer))
      (ctl/info "Starting new producer")
      (assoc component
             :producer (KafkaProducer. (doto (Properties.)
                                         (.putAll config))))))

  (stop
    [component]
    (when-let [producer (:producer component)]
      (ctl/info "Closing producer...")
      (.close producer)
      (dissoc component :producer)))

  KafkaProducerProtocol
  (send-event
    [component topic key value]
    (let [record (ProducerRecord. topic key value)
          producer (:producer component)]
      (.send producer record
             (reify Callback
               (onCompletion [_ metadata exception]
                 (if (some? exception)
                   (ctl/info (str "Error sending message: " (.getMessage exception)))
                   (ctl/info (str "Successfully sent message to topic " (.topic metadata)
                                  " at partition " (.partition metadata)
                                  " with offset " (.offset metadata))))))))))


(defn new-notification-producer
  [config]
  (->NotificationProducer (:message-kafka-producer-config config)))
