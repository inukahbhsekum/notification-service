(ns consumers.factory)

(defprotocol KafkaConsumer
  (start-consumer [this] "starts a consumer and event consumption")
  (stop-consumer [this] "stops a consumer")
  (handle-consumer-event [this] "handles an event received from consumer"))
