(ns server.components.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car]))

(defrecord Redis
    [config]
  component/Lifecycle
  (start [comp]
    comp)
  (stop [comp]
    comp))
