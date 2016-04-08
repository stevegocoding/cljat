(ns cljat-webapp.site
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [ajax.core :refer [GET POST]]))

(enable-console-print!)

(js/console.log "hahaljlsakjdflakjsf")

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

(let [login-ch (listen (dom/getElement "login-form") goog.events.EventType.SUBMIT)]
  (go (while true
        (let [e (<! login-ch)]
          (.preventDefault e)
          (let [form (dom/getElement "login-form")
                email (.-value (dom/getElement "email-input"))
                password (.-value (dom/getElement "password-input"))]
            (POST "/login" {:params {:email email
                                         :password password}
                                :handler (fn [resp]
                                           (go (<! (timeout 1000))
                                               (.submit form)))
                                :error-handler handler
                                :format :json
                                :response-format :json
                                :keywords? true}))))))
