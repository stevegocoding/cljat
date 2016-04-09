(ns cljat-webapp.components.ws-handler
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! put! take! chan close! go go-loop alts! alt!]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defn start-msg-recv-loop! [in-ch out-ch]
  (log/info "Starting ws handler recv msg process ...")
  (let [stop-ch (chan)]
    (go-loop []
      (let [[val ch] (alts! [in-ch stop-ch])]
        (when-not (= ch stop-ch)
          (let [{:keys [event] :as msg} val]
            (log/debug "ws-msg-recv-loop -- take a message from recv channel")
            (log/debug "msg: " event)
            (>! out-ch msg)
            (log/debug "ws-msg-recv-loop -- put a message onto outgoing channnel"))
         (recur))))
    
    (fn [] (put! stop-ch :stop))))

(defn start-msg-echo-loop! [out-ch send-fn]
  (go-loop []
    (let [{:keys [client-id ?data] :as msg} (<! out-ch)]
      (log/debug "echo msg: " msg)
      (send-fn client-id [:cljat.webapp.server/echo {:data (str "echo" (:data ?data))}])
      (recur))))


(defrecord WSHandler [ws-router-ch router-ws-ch]
  component/Lifecycle

  (start [component]
    (log/info "Starting WSHandler component ...")
    (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uuids]}
          (sente/make-channel-socket-server! sente-web-server-adapter {:user-id-fn (fn [ring-req] (:client-id ring-req))})
          stop-recv-fn (start-msg-recv-loop! ch-recv ws-router-ch)]

      (start-msg-echo-loop! ws-router-ch send-fn)
      (assoc component
             :recv-ch ch-recv
             :send-fn send-fn
             :ajax-post-fn ajax-post-fn
             :ws-handshake-fn ajax-get-or-ws-handshake-fn
             :client-uuids connected-uuids
             :stop-recv-fn stop-recv-fn)))

  (stop [component]
    (log/info "Stopping WSHandler component ...")

    ;; stop the go-loop process
    (when-let [stop-recv-fn! (:stop-recv-fn component)]
      (stop-recv-fn!))

    (assoc component
      :recv-ch nil
      :send-fn nil
      :ajax-post-fn nil
      :ws-handshake-fn nil
      :client-uuids nil
      :stop-recv-fn nil)))

(defn new-ws-handler []
  (map->WSHandler {}))
