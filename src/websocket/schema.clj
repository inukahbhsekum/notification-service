(ns websocket.schema)

(def websocket-message-type [:enum "connect" "echo" "send"])

(def WebsocketMessagePayload
  [:map
   [:topic_id string?]
   [:user_id string?]
   [:type websocket-message-type]])

(def EchoMessagePayload
  [:map
   [:topic_id string?]
   [:user_id string?]])
