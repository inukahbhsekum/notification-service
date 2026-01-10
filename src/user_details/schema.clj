(ns user-details.schema)

(def UserMetaData
  [:map
   [:ip_address string?]
   [:last_known_location string?]
   [:last_country string?]
   [:last_state string?]
   [:last_city string?]])

(def CreateUserRequest
  [:map
   [:first_name string?]
   [:middle_name string?]
   [:last_name string?]
   [:user_type [:enum "publisher" "receiver" "manager"]]
   [:user_metadata UserMetaData]])

(def CreateTopicRequest
  [:map
   [:title string?]
   [:description string?]
   [:user_id string?]])

(def CreateTopicUserMappingRequest
  [:map
   [:topic-id string?]
   [:user-ids [:vector {:min-count 1} string?]]
   [:manager-id string?]])

(def PasswordSchema
  [:and
   :string
   [:re {:error/message "Password must be at least 8 chars with one digit and one letter"}
    #"^(?=.*[A-Za-z])(?=.*\d).{8,}$"]])

(def UserLoginRequest
  [:map
   [:username string?]
   [:password PasswordSchema]])
