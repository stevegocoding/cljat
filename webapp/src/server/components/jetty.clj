(ns server.components.jetty
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord WebServer [options server handler]
  component/Lifecycle

  (start [component]
    (let [handler (get-in component [:handler :handler-fn] handler)
          server (run-jetty handler options)]
      (assoc component :server server)))
  
  (stop [component]
    (when server
      (.stop server)
      (assoc component :server nil))))

(defn new-web-server
  [options]
  (map->WebServer {:options options}))
