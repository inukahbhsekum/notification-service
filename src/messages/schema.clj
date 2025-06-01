(ns messages.schema
  (:require [schema.core :as sc]))

(def message-medium (sc/enum "websocket" "push_notification" "sms" "whatsapp" "email" "in_app" "message_queue" "webhook"))

(sc/defschema
  CreateMessageRequest
  {:message_text sc/Str
   :topic_id     sc/Str
   :sender       sc/Str
   :receiver     sc/Str
   :message_medium message-medium})


(sc/defschema
  SendMessageRequest
  {:message_id sc/Str
   :sender_id  sc/Str})


(sc/defschema
  FetchMessageRequest
  {:topic_id sc/Str
   :from Long
   :limit sc/Num
   :user_id sc/Str})
