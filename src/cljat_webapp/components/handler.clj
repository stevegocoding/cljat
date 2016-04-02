(ns cljat-webapp.components.handler
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as compojure]))

(defrecord Handler []
  component/Lifecycle
  (start [component]
    (let [routes (keep :routes (vals component))
          wrap-wm (get-in component [:middleware :wrap-wm] identity)
          handler (wrap-wm (apply compojure/routes routes))]
      (assoc component :handler-fn handler)))

  (stop [component]
    (do (dissoc component :handler-fn)
        component)))

(defn new-handler
  []
  (->Handler))
