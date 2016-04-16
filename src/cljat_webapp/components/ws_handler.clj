(ns cljat-webapp.components.ws-handler
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! put! take! chan close! go go-loop alts! alt!]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [cljat-webapp.components.redis :as r]))


(defmulti process-ws-msg (fn [ws-msg] (:id ws-msg)))

;; This is a system message that will be received when handshake is established
(defmethod process-ws-msg :chsk/uidport-open
  [ws-msg]
  (log/debug "ws message received: " (:id ws-msg) " user: " (:client-id ws-msg))
  (let [id (:id ws-msg)
        user-id (:client-id ws-msg)
        timestamp (tc/to-long (t/now))]
    [id {:data {:uid user-id :timestamp timestamp}}]))

;; This is a system message that will be received then a client is disconnected (eg. close the browser tab)
(defmethod process-ws-msg :chsk/uidport-close
  [ws-msg]
  (log/debug "ws message received :chsk/uidport-close : " (:id ws-msg) " user: " (:client-id ws-msg))
  (let [id (:id ws-msg)
        user-id (:client-id ws-msg)
        timestamp (tc/to-long (t/now))]
    [id {:data {:uid user-id :timestamp timestamp}}]))

(defmethod process-ws-msg :chsk/ws-ping
  [ws-msg]
  (log/debug "ws message received: " (:id ws-msg) " user: " (:client-id ws-msg))
  (let [id (:id ws-msg)
        user-id (:client-id ws-msg)
        timestamp (tc/to-long (t/now))]
    [id {:data {:uid user-id :timestamp timestamp}}]))

(defmethod process-ws-msg :cljat/user-join
  [ws-msg]
  (log/debug "ws message received: " (:id ws-msg) " user: " (:client-id ws-msg))
  (let [id (:id ws-msg)
        user-id (:client-id ws-msg)
        {msg-data :data} (:?data ws-msg)
        timestamp (tc/to-long (t/now))]
    [id {:data (assoc msg-data :uid user-id :timestamp timestamp)}]))

(defmethod process-ws-msg :cljat/chat-msg
  [ws-msg]
  (log/debug "ws message received: " (:id ws-msg) " user: " (:client-id ws-msg))
  (let [id (:id ws-msg)
        user-id (:client-id ws-msg)
        {msg-data :data} (:?data ws-msg)
        timestamp (tc/to-long (t/now))]
    [id {:data (assoc msg-data :uid user-id :timestamp timestamp)}]))

(defmethod process-ws-msg :cljat/add-friend
  [ws-msg]
  (log/debug "ws message received: " (:id ws-msg) " user: " (:client-id ws-msg))
  (let [id (:id ws-msg)
        user-id (:client-id ws-msg)
        {msg-data :data} (:?data ws-msg)
        timestamp (tc/to-long (t/now))]
    [id {:data (assoc msg-data :uid user-id :timestamp timestamp)}]))

(defn start-msg-recv-loop! [in-ch out-ch]
  (log/info "Starting ws handler recv msg process ...")
  (let [stop-ch (chan)]
    (go-loop []
      (let [[val ch] (alts! [in-ch stop-ch])]
        (when-not (= ch stop-ch)
          (log/debug "msg: " val)
          (when-let [msg (process-ws-msg val)]
            (>! out-ch msg))
         (recur))))
    
    (fn [] (put! stop-ch :stop))))

(defn start-msg-echo-loop! [out-ch send-fn redis]
  (go-loop []
    (let [[id {msg-data :data} :as msg-body] (<! out-ch)]
      (log/debug "echo msg: " "id: " id " data: "  msg-data)
      (log/debug "echo msg: " "sent to: " (:sent-to msg-data))
      (cond
        (= id :cljat/chat-msg) (do
                                 (let [client-ids (r/thread-members redis (str (:sent-to msg-data)))]
                                   (log/debug "echo msg -- receivers: " client-ids)
                                   (doseq [client-id client-ids]
                                     (log/debug "send msg: " client-id)
                                     (send-fn (str client-id) [id {:data msg-data}]))))
        (= id :cljat/add-friend) (do
                                   (let [client-ids [(:sent-from msg-data) (:sent-to msg-data)]]
                                     (log/debug "echo msg add friend  -- receivers: " client-ids)
                                     (doseq [client-id client-ids]
                                       (log/debug "send msg: " client-id)
                                       (send-fn (str client-id) [id {:data msg-data}])))))
      (recur))))


(defrecord WSHandler [ws-router-ch router-ws-ch redis]
  component/Lifecycle

  (start [component]
    (log/info "Starting WSHandler component ...")
    (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uuids]}
          (sente/make-channel-socket-server! sente-web-server-adapter {:user-id-fn (fn [ring-req] (str (:identity ring-req)))})
          stop-recv-fn (start-msg-recv-loop! ch-recv ws-router-ch)]

      (start-msg-echo-loop! router-ws-ch send-fn redis)
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
      :stop-recv-fn nil

      :ws-router-ch nil
      :router-ws-ch nil)))

(defn new-ws-handler []
  (map->WSHandler {}))
