(ns user-details.routes
  (:require [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [user-details.handlers :as handler]))


(def routes (route/expand-routes
              #{["/register-user"
                 :post [(body-params/body-params) handler/register-user-handler]
                 :route-name :register-user]}))


(def url-for (route/url-for-routes routes))
