(ns producers.notification-message-producer
  (:require [clojure.tools.logging :as ctl]
            [config :as config]
            [producers.factory :as pf])
  (:import (java.util Properties)
           (org.apache.kafka.clients.producer Callback KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer)))

(def message-producer nil)

(defrecord NotificationProducer [config state]
  pf/KafkaProducer
  (start-producer
    [this]
    (let [producer
          (KafkaProducer. (doto (Properties.)
                            (.putAll (merge config
                                            {"key.serializer"   StringSerializer
                                             "value.serializer" StringSerializer}))))]
      (reset! state {:producer producer})))

  (stop-producer
    [this]
    (ctl/info "Closing producer...")
    (.close (get-in @state [:producer])))

  (send-event
    [this topic key value]
    (let [record (ProducerRecord. topic key value)
          producer (get-in @state [:producer])]
      (.send producer record
             (reify Callback
               (onCompletion [_ metadata exception]
                 (if (some? exception)
                   (ctl/info (str "Error sending message: " (.getMessage exception)))
                   (ctl/info (str "Successfully sent message to topic " (.topic metadata)
                                  " at partition " (.partition metadata)
                                  " with offset " (.offset metadata))))))))))


(defn -main
  []
  (let [service-config (config/read-config)]
    (->NotificationProducer service-config
                            (atom nil))))
