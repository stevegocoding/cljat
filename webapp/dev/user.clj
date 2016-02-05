(ns user
  (:require [clojure.tools.namespace.repl :as tn]
            [ring.adapter.jetty :as rj]
            [webapp.core :refer :all]))

(defn start-server
  []
  (rj/run-jetty #'app-routes {:port 8080 :join? false}))
