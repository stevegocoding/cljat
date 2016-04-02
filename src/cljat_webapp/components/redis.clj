(ns cljat-webapp.components.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car]))

(defn init-listener [conn]
  (car/with-new-pubsub-listener (:spec conn)
    {"foo" (fn [[msg-type topic payload]] nil)}))

(defrecord Redis [config]
  component/Lifecycle
  
  (start [component]
    (let [host (:redis-host config)
          port (:redis-port config)
          conn {:pool {} :spec {:host host :port port}}]
      (assoc component
             :redis-conn conn
             :listener (init-listener conn))))
  
  (stop [component]
    (assoc component
           :redis-conn nil
           :listener nil)))


(defn new-redis [config]
  (->Redis config))

(defn sub-topic [comp topic handler-fn]
  #_(if-let [conn (:redis-conn comp)
           listener (:listener comp)]
    #_(car/with-open-listener listener
      )))
