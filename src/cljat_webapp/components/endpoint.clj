(ns cljat-webapp.components.endpoint
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [buddy.core.keys :as keys]))

(defrecord Endpoint [auth-config routes-fn]
  component/Lifecycle
  
  (start [component]
    (log/info "Starting Endpoint component ...")
    (let [privkey (->
                    (:private-key auth-config)
                    (io/resource)
                    (keys/private-key (:passphrase auth-config)))]
      (assoc component :handler (routes-fn (assoc component :privkey privkey)))))

  (stop [component]
    (log/info "Stopping Endpoint component ...")
    (assoc component :privkey nil :handler nil)))

(defn new-endpoint [auth-config routes-fn]
  (map->Endpoint {:auth-config auth-config :routes-fn routes-fn}))
