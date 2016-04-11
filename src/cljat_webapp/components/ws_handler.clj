(ns cljat-webapp.components.ws-handler
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! put! take! chan close! go go-loop alts! alt!]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]))


#_(defn process-msg [msg]
  (let [id])
  (cond
    (= msg )))


(defn start-msg-recv-loop! [in-ch out-ch]
  (log/info "Starting ws handler recv msg process ...")
  (let [stop-ch (chan)]
    (go-loop []
      (let [[val ch] (alts! [in-ch stop-ch])]
        (when-not (= ch stop-ch)
          (let [{:keys [id event ?data] :as msg} val]
            #_(log/debug "msg: " (:data ?data))
            ;; add message's timestamp
            (let [timestamp (tc/to-long (t/now))
                  msg-to-send (update-in msg [:?data :data :timestamp] (constantly timestamp))]
              (>! out-ch msg-to-send)))
         (recur))))
    
    (fn [] (put! stop-ch :stop))))

(defn start-msg-echo-loop! [out-ch send-fn]
  (go-loop []
    (let [{:keys [client-id id ?data] :as msg} (<! out-ch)]
      (log/debug "echo msg: " "id: " id " data: " ?data)
      (send-fn client-id [id ?data])
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
