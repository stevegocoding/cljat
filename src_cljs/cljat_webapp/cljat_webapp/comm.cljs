(ns cljat-webapp.comm
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! take! chan >! <! timeout close!]]
            [taoensso.sente :as sente]
            [ajax.core :refer [GET POST]]))

(defn init-ws! [url ch-out]
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client! url {:type :ws})
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
