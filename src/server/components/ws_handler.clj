(ns server.components.ws-handler
  (:require [clojure.core.async :refer [>! <! chan close! go go-loop alts!]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defn start-msg-loop! [in-chan out-chan]
  (let [ctrl-chan (chan)]
    (go-loop []
      (let [[val ch] (alts! [in-chan ctrl-chan])]
        (when-let [{:keys [event] :as msg} val]
          (>! out-chan msg)
          (recur))))
    
    ctrl-chan))

(defrecord WSHandler [ws-router-chan router-ws-chan]
  component/Lifecycle

  (start [component]
    (let [{:keys [recv-chan send-fn ajax-post-fn ws-handshake-fn connected-uuids]}
          (sente/make-channel-socket-server! sente-web-server-adapter {})
          ctrl-chan (start-msg-loop! recv-chan)]
      
      (assoc component
             :recv-chan recv-chan
             :send-fn send-fn
             :ajax-post-fn ajax-post-fn
             :ws-handshake-fn ws-handshake-fn
             :client-uuids connected-uuids
             :ctrl-chan ctrl-chan)))

  (stop [component]
    (do
      ;; stop the go-loop process
      (when-let [ctrl-chan (:ctrl-chan component)]
        (close! ctrl-chan))

      (dissoc component
              :recv-chan
              :send-fn
              :ajax-post-fn
              :ws-handshake-fn
              :client-uuids
              :ctrl-chan)
      
      (component))))

(defn new-ws-handler []
  (->WSHandler))
