(ns server.app-routes
  (:require [clojure.core.async :refer [<! >! put! take! close! thread go go-loop]]
            [clojure.tools.logging :as log]
            (compojure
             [core :refer [routes GET POST ANY]]
             [route :as route])
            [server.layout :as layout]))

;; App Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-route
  [request]
  (layout/render "home.html" {:home-title "cljat home"}))


(defn new-app-routes
  [{:keys [ws-handler] :as endpoint-comp}]
  (let [ws-handshake-fn (:ws-handshake-fn ws-handler)]
    (routes (GET "/" request (home-route request))
            (GET "/ws" req (ws-handshake-fn req))
            #_(route/not-found (slurp (io/resource "404.html"))))))
