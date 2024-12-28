(ns websocket.handler
  (:require [clojure.tools.logging :as ctl]
            [org.httpkit.server :as http]))

(defn websocket-handler
  [request]
  (ctl/info "-->" request)
  (http/with-channel request channel
    ;; On connect
    (ctl/info "Client connected!")
    ;; Setup receive listener
    (http/on-receive channel
                     (fn [msg]
                       (ctl/info "Received message from client:" msg)
                       ;; Echo the message back to the client
                       (http/send! channel (str "Echo: " msg))))
    ;; Setup close listener
    (http/on-close channel
                   (fn [status]
                     (ctl/info "Client disconnected with status:" status)))))
