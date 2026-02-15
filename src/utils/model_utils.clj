(ns utils.model-utils
  (:require [java-time.api :as jt])
  (:import [java.util UUID]))

(defn timestamp
  []
  (jt/format "ddMMyyyyHHmmss" (jt/local-date-time)))


(defn gen-identifier
  [vertical entity]
  (str vertical "_" entity "_" (timestamp) "_" (UUID/randomUUID)))
