(ns messages.message-queue
  (:import [clojure.lang PersistentQueue]))

(def ^:private message-queue (atom {}))


(defn enqueue-message
  "It enqueues the message-id to the message-queue of the topic
   messages for the topic-id"
  [topic-id message-id]
  (swap! message-queue update topic-id (fn [topic-messages]
                                         (if (seq topic-messages)
                                           (conj topic-messages message-id)
                                           (conj PersistentQueue/EMPTY message-id)))))
