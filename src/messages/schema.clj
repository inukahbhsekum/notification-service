(ns messages.schema)

(def message-mediums [:enum "websocket" "push_notification" "sms" "whatsapp" "email" "in_app" "message_queue" "webhook"])

(def CreateMessageRequest
  [:map
   [:message_text string?]
   [:topic_id string?]
   [:sender string?]
   [:receiver string?]
   [:message_medium message-mediums]])


(def SendMessageRequest
  [:map
   [:message_id string?]
   [:sender_id string?]])


(def FetchMessageRequest
  [:map
   [:topic_id string?]
   [:from int?]
   [:limit [:and int? [:> 0]]]
   [:user_id string?]])
