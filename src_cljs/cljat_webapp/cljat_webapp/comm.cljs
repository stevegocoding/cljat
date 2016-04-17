(ns cljat-webapp.comm
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.walk :as walk]
            [cljs.core.async :refer [put! take! chan >! <! timeout close!]]
            [taoensso.sente :as sente]
            [ajax.core :refer [GET POST]]))

(defn init-ws! [url ch-out user-id]
  (js/console.log "client: " user-id)
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client! url {:type :ws
                                                :client-id user-id})
        ch-in (chan)
        ch-stop (chan)]
    (go-loop []
      (let [[v ch] (alts! [ch-out ch-recv ch-stop])]
        (when-not (= ch ch-stop)
          (cond
            (= ch ch-recv) (do
                             (js/console.log "receiving msg ..." v)
                             (>! ch-in v))
            (= ch ch-out) (do
                            (js/console.log "sending msg ..." v)
                            (send-fn v)))
          (recur))))
    {:ch-in ch-in
     :stop-ws (fn [] (put! ch-stop :no-op))}))

(defn ajax-chan [ajax-fn url {:as params}]
  (let [out (chan)]
    (ajax-fn url {:params params
                  :handler (fn [resp] (put! out resp))
                  :error-handler (fn [resp] (put! out resp))
                  :format :json
                  :response-format :json})
    out))

(defn fetch-friends-list [user-id]
  (go
    (js/console.log "ajax -- fetch friends list")
    (let [resp (<! (ajax-chan GET "/app/friends-info" {:user-id user-id}))]
      (->
        (js->clj resp)
        (walk/keywordize-keys)
        (get-in [:data])))))

(defn fetch-threads-list [user-id]
  (go
    (js/console.log "ajax -- fetch threads list")
    (let [resp (<! (ajax-chan GET "/app/threads-info" {:user-id user-id}))]
      (->
        (js->clj resp)
        (walk/keywordize-keys)
        (get-in [:data])))))

(defn add-thread [user-id friend-id]
  (go
    (js/console.log "ajax -- create new chat")
    (let [resp (<! (ajax-chan POST "/app/add-thread" {:user-id user-id
                                                       :friend-id friend-id}))]
      (->
        (js->clj resp)
        (walk/keywordize-keys)
        (get-in [:data])))))
