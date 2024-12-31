(ns messages.schema
  (:require [schema.core :as sc]))

(def websocket-message-type (sc/enum "connect" "echo"))


(sc/defschema
  CreateMessageRequest
  {:message_text sc/Str
   :topic_id     sc/Str
   :sender       sc/Str
   :receiver     sc/Str})


(sc/defschema
  SendMessageRequest
  {:message_text sc/Str
   :topic_id sc/Str
   :manager_id sc/Str})


(sc/defschema
  WebsocketMessagePayload
  {:message_body sc/Str
   :type websocket-message-type})
