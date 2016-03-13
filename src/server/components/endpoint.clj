(ns server.components.endpoint
  (:require [com.stuartsierra.component :as component]))

(defrecord Endpoint [routes-fn]
  component/Lifecycle
  (start [component]
    (assoc component :routes (routes-fn component)))

  (stop [component]
    (do (dissoc component :routes)
        component)))

(defn new-endpoint [routes-fn]
  (->Endpoint routes-fn))
