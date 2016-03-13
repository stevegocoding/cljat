(ns server.app-routes
  (:require [compojure.route :as route]
            [compojure.core :refer [routes GET POST ANY]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

;; App Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-route
  [request]
  (quot 1 1)
  (str request))

(defn new-app-routes
  [handler]
  (routes (GET "/" request (home-route request))
          #_(route/not-found (slurp (io/resource "404.html")))))
