(ns components.kafka-components
  (:require [clojure.tools.logging :as ctl]
            [config :as conf])
  (:import (java.util Properties)
           (org.apache.kafka.clients.producer Callback KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer)))


(defn create-producer
  "Creates and returns a new KafkaProducer instance.
   The producer is configured with the provided properties."
  []
  (KafkaProducer. (doto (Properties.)
                    (.putAll (merge (:kafka-config (conf/read-config))
                                    {"key.serializer"   StringSerializer
                                     "value.serializer" StringSerializer})))))


(defn send-message
  "Sends a message asynchronously to a Kafka topic.
   Takes the producer instance, topic name, key, and value.
   Includes an optional callback to handle the result of the send operation."
  [^KafkaProducer producer topic key value]
  (let [record (ProducerRecord. topic key value)]
    (.send producer record
           (reify Callback
             (onCompletion [_ metadata exception]
               (if (some? exception)
                 (ctl/info (str "Error sending message: " (.getMessage exception)))
                 (ctl/info (str "Successfully sent message to topic " (.topic metadata)
                                " at partition " (.partition metadata)
                                " with offset " (.offset metadata)))))))))


(defn close-producer
  "Closes the KafkaProducer instance, flushing any pending messages."
  [^KafkaProducer producer]
  (ctl/info "Closing producer...")
  (.close producer))
