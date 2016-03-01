(ns server.components.handler
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as compojure]))

(defrecord Handler [routes middleware]
  component/Lifecycle

  (start [comp]
    (let [routes (filter some (vals comp))
          wrap-mw (get-in comp [:middleware :wrap-wm] identity)
          handler (wrap-wm (apply compojure/routes routes))]
      (assoc comp :handler handler)))

  (stop [comp]
    (assoc comp :handler nil)))


(defn new-handler
  []
  (->Handler))
