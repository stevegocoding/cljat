(ns cljat-webapp.components.msg-echo
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! chan close! go go-loop alts!]]
            [com.stuartsierra.component :as component]))

(defn msg-echo-loop [in-ch out-ch]
  (log/info "Starting message echo go loop ...")
  (go-loop []
    (when-let [msg (<! in-ch)]
      (log/info "msg-echo -- take a message from input channel")
      (>! out-ch msg)
      (log/info "msg-echo -- put a message onto outgoing channel")
      (recur))))

(defrecord MessageEcho [in-ch out-ch]
  component/Lifecycle

  (start [component]
    (log/info "Starting MessageEcho component ...")
    (msg-echo-loop in-ch out-ch)
    component)

  (stop [component]
    (log/info "Stopping MessageEcho component ...")
    component))

(defn new-msg-echo []
  (map->MessageEcho {}))
