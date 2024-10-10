(ns utils.convertor-utils
  (:require [clojure.data.json :as json])
  (:import (org.postgresql.util PGobject)))


(defn sql-json->
  [data]
  (when data
    (doto (PGobject.)
      (.setType "json")
      (.setValue (json/write-str data)))))
