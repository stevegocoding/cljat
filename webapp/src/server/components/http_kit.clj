(ns server.components.http-kit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]))


(defrecord WebServer [options server handler]
  component/Lifecycle
  
  (start [comp]
    (let [server (run-server handler options)]
      (assoc comp :server server)))

  (stop [comp]
    (assoc comp :server nil)))


(defn new-web-server
  ([options]
   (new-web-server 8080 options))
  ([port options]
   (map->WebServer {:options (merge {:port port} options)})))
