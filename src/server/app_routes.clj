(ns server.app-routes
  (:require [compojure.route :as route]
            [compojure.core :refer [routes GET POST ANY]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [server.layout :as layout]))

;; App Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-route
  [request]
  (layout/render "home.html" {:home-title "cljat home"}))

(defn new-app-routes
  [handler]
  (routes (GET "/" request (home-route request))
          #_(route/not-found (slurp (io/resource "404.html")))))
