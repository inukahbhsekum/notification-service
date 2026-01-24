(ns components.kafka-components
  (:require [clojure.tools.logging :as ctl])
  (:import (java.util Properties)
           (java.util Properties Collections)
           (org.apache.kafka.clients.consumer ConsumerConfig KafkaConsumer Consumer ConsumerRecords)
           (org.apache.kafka.clients.producer Callback KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer)))


(defn create-producer
  "Creates and returns a new KafkaProducer instance.
   The producer is configured with the provided properties."
  [config]
  (KafkaProducer. (doto (Properties.)
                    (.putAll (merge config
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


(comment
  (defn close-producer
    "Closes the KafkaProducer instance, flushing any pending messages."
    [^KafkaProducer producer]
    (ctl/info "Closing producer...")
    (.close producer)))


(defn create-consumer
  "Creates and returns a new KafkaConsumer instance.
   The consumer is configured with the provided properties."
  [config]
  (let [props (Properties.)]
    (.put props ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG (:bootstrap-servers config))
    (.put props ConsumerConfig/GROUP_ID_CONFIG (:group-id config))
    (.put props ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG
          "org.apache.kafka.common.serialization.StringDeserializer")
    (.put props ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG
          "org.apache.kafka.common.serialization.StringDeserializer")
    (let [consumer (KafkaConsumer. props)]
      (.subscribe consumer (Collections/singletonList (:topic-name config)))
      consumer)))


(defn consume-messages
  "The core blocking loop which takes the consumer object along with,
   stop-flag: a stop-flag for notifying the consumer to stop 
              message consumtion
   topic-name: topic-name for consumption of messages
   consumer-poll-time: time to recheck for new-messages"
  [^Consumer consumer stop-flag topic-name consumer-poll-time]
  (.subscribe consumer (Collections/singletonList topic-name))
  (ctl/log :info (str "Consumer started and subscribed to:" topic-name))
  (while (not @stop-flag)
    (try
      (let [^ConsumerRecords records (.poll consumer (java.time.Duration/ofMillis consumer-poll-time))]
        (doseq [record records]
          ;; --- YOUR MESSAGE PROCESSING LOGIC HERE ---
          (ctl/info "Processing message: " record)
          (ctl/log :info
                   (format "Partition: %d, Offset: %d, Key: %s, Value: %s"
                           (.partition record)
                           (.offset record)
                           (.key record)
                           (.value record))))
        (.commitAsync consumer))
      (catch Exception e
        (ctl/log :info "Error during consumption:" (.getMessage e)))))
  (ctl/log :info "Closing consumer gracefully...")
  (.close consumer)
  (ctl/log :info "Consumer closed.")
  nil)
