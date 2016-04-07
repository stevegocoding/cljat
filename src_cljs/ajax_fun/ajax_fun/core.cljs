(ns ajax-fun.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]]
            [ajax.core :refer [GET POST]]))

(enable-console-print!)

(.log js/console "Welcome to ajax-fun!")

(defn fig-reload []
  (.log js/console "figwheel reloaded!"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (do 
                (.preventDefault e)
                (put! out e))))
    out))

(defn handler [response]
  (.log js/console "server responded..."))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(let [login-ch (listen (dom/getElement "ajax-btn") goog.events.EventType.CLICK)]
  (go (while true
        (<! login-ch)
        (POST "/ctx1/doajax" {:params {:name "haha"}
                         :handler handler
                         :error-handler handler
                         :format :json
                         :response-format :json
                         :keywords? true}))))

(let [login-ch (listen (dom/getElement "ajax-btn2") goog.events.EventType.CLICK)]
  (go (while true
        (<! login-ch)
        (POST "/ctx2/doajax2" {:params {:name "haha"}
                          :handler handler
                          :error-handler handler
                          :format :json
                          :response-format :json
                          :keywords? true}))))
