(ns messages.schema
  (:require [schema.core :as sc]))


(sc/defschema
  CreateMessageRequest
  {:message_text sc/Str
   :topic_id     sc/Str
   :sender       sc/Str
   :receiver     sc/Str})
