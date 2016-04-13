(ns cljat-webapp.components.msg-router
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! put! take! chan close! go go-loop alts! alt!]]
            [cljat-webapp.components.redis :as r]))


;; A messages router component that utilize redis to pub messags
;; across multiple web servers

(defn listen-redis-pub [out-ch]
  (fn [[type channel content :as msg]]
    (log/debug "handle redis pub: ")
    (log/debug "type: " type " channel: " channel " content: " content)
    (cond
      (= (get content 0) :cljat/chat-msg) (put! out-ch content))))

(defn handle-msg [redis listener [id {msg-data :data} :as msg-body]]
  (cond
    (= id :chsk/uidport-open) (log/debug "msg route: " "user connected!" " data: " msg-data)
    (= id :cljat/user-join) (do
                              (log/debug "msg route: " "user joined!" " data: " msg-data)
                              ;; register this user's threads
                              (r/cache-user-info redis (:uid msg-data) (:tids msg-data) listener)
                              #_(doseq [thread-id (:tids msg-data)]
                                (r/add-user-to-thread redis thread-id (:uid msg-data))
                                (r/sub-thread redis thread-id listener)))
    (= id :cljat/chat-msg) (do
                             (log/debug "msg route: " "chat msg received!" " data: " msg-data)
                             (r/pub-msg redis (:sent-to msg-data) msg-body))
    (= id :chsk/uidport-close) (do
                                 (log/debug "msg route: " "user disconnected!" " data: " msg-data)
                                 ;; remove the user from thread key when logged out
                                (r/cleanup-user-info redis (:uid msg-data))
                                 #_(doseq [thread-id (:tids msg-data)]
                                   (r/remove-user-from-thread redis thread-id (:uid msg-data))))))

(defn start-msg-pub-loop! [in-ch out-ch redis]
  (log/info "Starting msg-router pub process ...")
  (let [stop-ch (chan)]
    (go-loop []
      (let [[val ch] (alts! [in-ch stop-ch])]
        (when-not (= ch stop-ch)
          #_(log/debug "msg router recv: " val)
          (let [[id {msg-data :data} :as msg-body] val]
            #_(log/debug "msg router recv: id - " id " data: " msg-data)
            (handle-msg redis (listen-redis-pub out-ch) msg-body))
          (recur))))
    
    (fn [] (put! stop-ch :stop))))

(defrecord MessageRouter [in-ch out-ch redis]
  component/Lifecycle

  (start [component]
    (log/info "Starting MessageRouter component ...")
    (start-msg-pub-loop! in-ch out-ch redis))

  (stop [component]
    (log/info "Stopping  MessageRouter component ...")
    (assoc component
      :in-ch nil
      :out-ch nil
      :redis nil)))

(defn new-msg-router []
  (map->MessageRouter {}))
