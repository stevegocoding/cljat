(ns cljat-webapp.system
  (:require [com.stuartsierra.component :as component]))

(defn system
  [config]
  )

(defn start-sys
  [s]
  (component/start-system s))

(defn stop-sys
  [s]
  (component/stop-system s))
