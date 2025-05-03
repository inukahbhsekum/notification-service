(ns utils.convertor-utils
  (:require [clj-time.coerce :as ctc]
            [clojure.data.json :as json])
  (:import (org.postgresql.util PGobject)))


(defn sql-json->
  [data]
  (when data
    (doto (PGobject.)
      (.setType "json")
      (.setValue (json/write-str data)))))


(defn millisecond-to-datetime
  [millisecond]
  (when millisecond
    (ctc/from-long millisecond)))
