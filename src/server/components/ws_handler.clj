(ns server.components.ws-handler
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! chan close! go go-loop alts!]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defn start-msg-loop! [in-ch out-ch]
  (log/info "Starting ws handler msg go loop ...")
  (let [ctrl-ch (chan)]
    (go-loop []
      (let [[val ch] (alts! [in-ch ctrl-ch])]
        (when-let [{:keys [event] :as msg} val]
          (log/info "ws-msg-loop -- take a message from recv channel")
          (>! out-ch msg)
          (log/info "ws-msg-loop -- put a message onto outgoing channnel")
          (recur))))
    
    ctrl-ch))

(defrecord WSHandler [ws-router-ch router-ws-ch]
  component/Lifecycle

  (start [component]
    (log/info "Starting WSHandler component ...")
    (let [{:keys [recv-ch send-fn ajax-post-fn ws-handshake-fn connected-uuids]}
          (sente/make-channel-socket-server! sente-web-server-adapter {})
          ctrl-ch (start-msg-loop! recv-ch ws-router-ch)]
      
      (assoc component
             :recv-ch recv-ch
             :send-fn send-fn
             :ajax-post-fn ajax-post-fn
             :ws-handshake-fn ws-handshake-fn
             :client-uuids connected-uuids
             :ctrl-ch ctrl-ch)))

  (stop [component]
    (log/info "Stopping WSHandler component ...")
    (do
      ;; stop the go-loop process
      (when-let [ctrl-ch (:ctrl-ch component)]
        (close! ctrl-ch))

      (dissoc component
              :recv-ch
              :send-fn
              :ajax-post-fn
              :ws-handshake-fn
              :client-uuids
              :ctrl-ch)
      
      component)))

(defn new-ws-handler []
  (map->WSHandler {}))
