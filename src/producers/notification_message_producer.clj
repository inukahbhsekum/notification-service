(ns producers.notification-message-producer
  (:gen-class)
  (:require [nrepl.server :as nrepl]))


(defonce ^{:doc ""}
  kafka-producer nil)


(defn init-producer
  "Initialise kafka producer from the config"
  [{:keys [config opts monitoring]}]
  (let [monitoring-config ()]))


(defn -main
  "Eba producer setup"
  []
  ())
