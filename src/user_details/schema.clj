(ns user-details.schema
  (:require [clojure.string :as cs]
            [clojure.set :as cset]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.generator :as mg]))


(def create-user-request
  [:map
   [:first_name [:string [:min 1 :max 50]]]
   [:middle_name :string]
   [:last_name [:string [:min 1 :max 50]]]
   [:user_type [:enum "publisher" "receiver" "manager"]]
   [:user_metadata []]])


(mg/generate create-user-request)