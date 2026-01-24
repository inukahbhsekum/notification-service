(ns consumers.notification-message-consumer
  (:require [clojure.tools.logging :as ctl]
            [components.kafka-components :as ckc]
            [config :as config])
  (:import (org.apache.kafka.clients.consumer ConsumerRecord)))

(def consumer-instance nil)
(def consumer-thread (atom ConsumerRecord))
(def stop-flag (atom false))

(defn create-notification-message-consumer
  [config]
  (ctl/info "Creating Notification Message Consumer..." config)
  (let [consumer (ckc/create-consumer (:message-kafka-consumer-config config))
        {:keys [topic-name]} (:message-kafka-consumer-config config)]
    (alter-var-root #'consumer-instance (constantly consumer))
    (reset! stop-flag false)
    (let [worker (future
                   (ckc/consume-messages consumer
                                         stop-flag
                                         topic-name
                                         (:consumer-poll-time config)))]
      (alter-var-root #'consumer-thread (constantly worker)))
    (ctl/log :info "Notification Message Consumer started in background thread.")
    consumer-instance))


(comment
  (defn stop-notification-message-consumer
    "Gracefully stops the consumer by setting the stop flag and waiting for the thread."
    []
    (when @consumer-thread
      (ctl/log :info "Attempting to stop Notification Message Consumer...")

      ;; 3. Set the stop flag. The ckc/consume-messages loop will see this and exit.
      (reset! stop-flag true)

      ;; Wait for the consumer thread to finish its loop and close the consumer.
      (try
        @(deref consumer-thread 5000 :timeout) ; Wait up to 5 seconds
        (catch Exception e
          (ctl/log :info (str "Error while waiting for consumer thread to stop:" (.getMessage e)))))

      (alter-var-root #'consumer-thread (constantly nil))
      (alter-var-root #'consumer-instance (constantly nil))
      (ctl/log :info "Notification Message Consumer stopped."))))


(defn -main
  []
  (let [service-config (config/read-config)]
    (create-notification-message-consumer service-config)))
