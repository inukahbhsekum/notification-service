(ns user-details.routes
  (:require [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [user-details.handlers :as handler]))


(def routes (route/expand-routes
              #{["/register-user"
                 :post [(body-params/body-params) handler/register-user]
                 :route-name :register-user-handler]
                ["/fetch-user"
                 :get [(body-params/body-params) handler/get-user]
                 :route-name :fetch-user-handler]
                ["/create-topic"
                 :post [(body-params/body-params) handler/create-topic]
                 :route-name :create-topic-handler]
                ["/create-topic-mapping"
                 :post [(body-params/body-params) handler/create-topic-user-mapping]
                 :route-name :create-topic-user-mapping]}))


(def url-for (route/url-for-routes routes))
