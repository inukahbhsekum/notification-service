(ns websocket.schema
  (:require [schema.core :as sc]))

(def websocket-message-type (sc/enum "connect" "echo" "send"))

(sc/defschema
  WebsocketMessagePayload
  {:topic_id sc/Str
   :user_id sc/Str
   :type websocket-message-type})

(sc/defschema
  EchoMessagePayload
  {:topic_id sc/Str
   :user_id sc/Str})
