(ns server.components.middleware
  (:require [com.stuartsierra.component :as component]))

(defn- middleware-fn
  [entry]
  (let [[f & opts] entry]
    (if opts
      #(apply f % opts)
      f)))

(defn- compose-middleware
  [middleware]
  (let [entries middleware]
    (->> (reverse entries)
         (map #(middleware-fn %))
         (apply comp identity))))

(defrecord Middleware [middleware]
  component/Lifecycle
  (start [component]
    (let [wrap-wm (compose-middleware middleware)]
      (assoc component :wrap-wm wrap-wm)))

  (stop [component]
    (do (dissoc component :wrap-wm)
        component)))

(defn new-middleware
  [middleware]
  (->Middleware middleware))
