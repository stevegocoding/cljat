(ns webapp.core
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [compojure.route :as route]))

(defn foo [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "hello World"})

(defroutes app-routes
  (GET "/" [] foo))
