(ns cljat-webapp.components.endpoint
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [buddy.core.keys :as keys]
            [buddy.auth.backends.token :refer [jws-backend]]))

(defrecord Endpoint [auth-config routes-fn]
  component/Lifecycle
  
  (start [component]
    (log/info "Starting Endpoint component ...")
    (let [handler (routes-fn component)]
      (assoc component :handler handler)))

  (stop [component]
    (log/info "Stopping Endpoint component ...")
    (assoc component :privkey nil :pubkey nil :handler nil routes-fn nil)))

(defn new-endpoint [auth-config routes-fn]
  (map->Endpoint {:auth-config auth-config :routes-fn routes-fn}))
