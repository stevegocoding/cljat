(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]))

(def sys nil)

(defn sys-config
  []
  {:log {:logger-name (env :logger-name)}})

#_(defn dev-system
  [config]
  (component/system-map
   :log (log/log-comp (:log config))))

(defn dev-system
  [config]
  (component/system-map {}))


(defn start-sys
  [s]
  (component/start-system s))

(defn stop-sys
  [s]
  (component/stop-system s))

(defn init
  ;; Construct the current development system
  []
  (alter-var-root #'sys
                  (constantly (dev-system (sys-config)))))

(defn start
  ;; Starts the current development system
  []
  (alter-var-root #'sys start-sys))

(defn stop
  ;; Shuts down and destroy the current development system
  []
  (alter-var-root #'sys
                  (fn [s] (when s (stop-sys s)))))

(defn go
  ;; Initialize the current development system
  []
  (init)
  (start))

(defn reset
  ;; Reset the current development system
  []
  (stop)
  (refresh :after 'user/go))
