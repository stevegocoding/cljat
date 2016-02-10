(ns server.components.log
  (:require [com.stuartsierra.component :as component])
  (:import [org.slf4j Logger LoggerFactory]))

(defrecord Log
    [logger-name]
  component/Lifecycle
  (start [comp]
    (println "Initializing logger ...")
    (println "logger config: " logger-name)
    (let [^org.slf4j.Logger logger (LoggerFactory/getLogger logger-name)]
      ;; Return an updated version of the component with the run-time state assoc'd in
      (assoc comp :logger logger))
    )
  (stop [comp]
    (println "Desctructing logger ...")

    ;; Return the modified component
    ;; assoc - returns our record type
    ;; dissoc - returns a map
    (assoc comp :logger nil)))

(defn log-comp
  ;; Create new log component
  [logger-name]
  (map->Log {:logger-name logger-name}))
