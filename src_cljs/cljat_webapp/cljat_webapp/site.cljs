(ns cljat-webapp.site
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [ajax.core :refer [GET POST]]))

(enable-console-print!)

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

(when-let [el (dom/getElement "login-form")]
  (let [login-ch (listen el goog.events.EventType.SUBMIT)]
    (go (while true
          (let [e (<! login-ch)
                login-btn (dom/getElement "login-btn")]
            (.preventDefault e)
            (set! (.-innerHTML login-btn) "Logging in ...")
            (set! (.-disabled login-btn) true)
            (let [form (dom/getElement "login-form")
                  email (.-value (dom/getElement "email-input"))
                  password (.-value (dom/getElement "password-input"))]
              (POST "/login" {:params {:email email
                                       :password password}
                              :handler (fn [resp]
                                         (js/console.log "login ok!")
                                         (go (<! (timeout 500))
                                             (.submit form)))
                              :error-handler handler
                              :format :json
                              :response-format :json
                              :keywords? true})))))))

(when-let [el (dom/getElement "signup-form")]
  (let [signup-ch (listen el goog.events.EventType.SUBMIT)]
    (go (while true
          (let [e (<! signup-ch)
                signup-btn (dom/getElement "signup-btn")]
            (set! (.-innerHTML signup-btn) "Processing ...")
            (set! (.-disabled signup-btn) true)
            (let [form (dom/getElement "signup-form")
                  nickname (.-value (dom/getElement "nickname-input"))
                  email (.-value (dom/getElement "email-input"))
                  password (.-value (dom/getElement "password-input"))]
              (<! (timeout 500))
              (POST "/signup2" {:params {:nickname nickname
                                        :email email
                                        :password password}
                               :handler (fn [resp]
                                          (js/console.log "signup ok!")
                                          (set! (.-innerHTML signup-btn) "Success!")
                                          (go (<! (timeout 900))
                                              (.submit form)))
                               :error-handler handler
                               :format :json
                               :response-format :json
                               :keywords? true})))))))
