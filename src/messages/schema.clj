(ns messages.schema
  (:require [schema.core :as sc]))

(sc/defschema
  CreateMessageRequest
  {:message_text sc/Str
   :topic_id     sc/Str
   :sender       sc/Str
   :receiver     sc/Str})


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
