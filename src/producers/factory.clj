(ns producers.factory)

(defprotocol KafkaProducer
  (start-producer [this] "starts a producer and event consumption")
  (stop-producer [this] "stops a producer")
  (send-event [this topic key value] "send an event to a producer"))
