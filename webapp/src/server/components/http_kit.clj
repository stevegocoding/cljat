(ns server.components.http-kit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]))

(defrecord WebServer [options handler server]
  component/Lifecycle
  
  (start [component]
    (let [server (run-server handler options)]
      (assoc component :server server)))

  (stop [component]
    (when server
      (reset! server nil)
      (assoc component :server nil))))

(defn new-web-server
  [options]
  (map->WebServer options))
