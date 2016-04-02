(ns cljat-webapp.components.msg-router
  (:require [com.stuartsierra.component :as component]))


;; A messages router component that utilize redis to pub messags
;; across multiple web servers
(defrecord MessageRouter []
  component/Lifecycle

  (start [component]
    )

  (stop [component]
    ))


(defn new-msg-router []
  (->MessageRouter []))
