(ns webapp.core
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [compojure.route :as route]
   [ring.util.response :as response]))

(defn foo [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "hello World zz "})

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"})))
