(ns server.app-routes
  (:require
   [compojure.route :as route]
   [compojure.core :refer [routes GET POST ANY]]))

(defn new-app-routes
  [handler]
  (routes
   (GET "/" [] "Welcome! This is cljat!")
   (route/not-found "404")))
