(ns server.components.chat-server
  (:require [com.stuartsierra.component :as component]
            [aleph.http :as http]))

(defrecord ChatServer
    [config redis]
  component/Lifecycle
  (start [comp]
    comp)
  (stop [comp]
    comp))

