(ns user-details.schema
  (:require [schema.core :as sc]))


(sc/defschema
  UserMetaData
  {:ip_address          sc/Str
   :last_known_location sc/Str
   :last_country        sc/Str
   :last_state          sc/Str
   :last_city           sc/Str})


(sc/defschema
  CreateUserRequest
  {:first_name    sc/Str
   :middle_name   sc/Str
   :last_name     sc/Str
   :user_type     (sc/enum "publisher" "receiver" "manager")
   :user_metadata UserMetaData})
