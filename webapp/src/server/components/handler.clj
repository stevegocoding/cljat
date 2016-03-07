(ns server.components.handler
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as compojure]))

(defrecord Handler [new-app-routes-fns handler-fn]
  component/Lifecycle

  (start [component]
    (let [app-handler (->> new-app-routes-fns
                           (map #(% component))
                           (apply compojure/routes))]
      (assoc component :handler-fn app-handler)))

  (stop [component]
    (assoc component :handler-fn nil)))

(defn new-handler
  [[:as fns]]
  (map->Handler {:new-app-routes-fns fns}))
