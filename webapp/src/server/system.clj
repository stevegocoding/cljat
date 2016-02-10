(ns server.system
  (:require [com.stuartsierra.component :as component]
            [server.components.log :as log]))

(defn system
  [config]
  (component/system-map
   :log (log/log-comp (:log config))))

(defn start-sys
  [s]
  (component/start-system s))

(defn stop-sys
  [s]
  (component/stop-system s))
