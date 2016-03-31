(ns server.components.endpoint
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defrecord Endpoint [routes-fn]
  component/Lifecycle
  
  (start [component]
    (log/info "Starting Endpoint component ...")
    (assoc component :routes (routes-fn component)))

  (stop [component]
    (log/info "Stopping Endpoint component ...")
    (do (dissoc component :routes)
        component)))

(defn new-endpoint [routes-fn]
  (->Endpoint routes-fn))
