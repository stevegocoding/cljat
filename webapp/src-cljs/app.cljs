(ns app
  (:require [figwheel.client :as fw]))

(fw/watch-and-reload
 :websocket-url   "ws://localhost:3449/figwheel-ws"
 :jsload-callback
 (fn []
   (println "reloaded")))

(.log js/console "Hello!!!")

