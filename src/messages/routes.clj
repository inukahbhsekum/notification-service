(ns messages.routes
  (:require [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [messages.handler :as handler]))


(def routes (route/expand-routes
             #{["/create-message"
                :post [(body-params/body-params) handler/create-message-handler]
                :route-name :create-message-handler]
               ["/send-message"
                :post [(body-params/body-params) handler/send-message-handler]
                :route-name :send-message-handler]}))


(def url-for (route/url-for-routes routes))
