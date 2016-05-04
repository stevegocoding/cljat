(ns cljat-webapp.site
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.style :as style]
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

(defn validate-email [email]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (re-matches pattern email)))

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
          (<! signup-ch)
          (let [signup-btn (dom/getElement "signup-btn")
                email-input-help (dom/getElement "email-input-help")
                nickname-input-help (dom/getElement "nickname-input-help")
                password-input-help (dom/getElement "password-input-help")
                form (dom/getElement "signup-form")
                nickname (.-value (dom/getElement "nickname-input"))
                email (.-value (dom/getElement "email-input"))
                password (.-value (dom/getElement "password-input"))
                signup-btn-text (.-innerHTML signup-btn)]
            (cond
              (empty? nickname) (do
                                  (set! (.-innerHTML nickname-input-help) "Nickname cannot be empty")
                                  (style/setStyle nickname-input-help "display" "block")
                                  (style/setStyle email-input-help "display" "none")
                                  (style/setStyle password-input-help "display" "none"))
              (empty? email) (do
                               (set! (.-innerHTML email-input-help) "Email cannot be empty")
                               (style/setStyle email-input-help "display" "block")
                               (style/setStyle nickname-input-help "display" "none")
                               (style/setStyle password-input-help "display" "none"))
              (empty? password) (do
                                  (set! (.-innerHTML password-input-help) "Password cannot be empty")
                                  (style/setStyle password-input-help "display" "block")
                                  (style/setStyle email-input-help "display" "none")
                                  (style/setStyle nickname-input-help "display" "none"))
              (nil? (validate-email email)) (do
                                              (set! (.-innerHTML email-input-help) "Invalid email format!")
                                              (style/setStyle email-input-help "display" "block")
                                              (set! (.-disabled signup-btn) false)
                                              (set! (.-innerHTML signup-btn) signup-btn-text))
              :else (do
                      (set! (.-innerHTML signup-btn) "Processing ...")
                      (set! (.-disabled signup-btn) true)
                      (style/setStyle email-input-help "display" "none")
                      (style/setStyle password-input-help "display" "none")
                      (style/setStyle nickname-input-help "display" "none")
                      (<! (timeout 500))
                      (POST "/signup2" {:params {:nickname nickname
                                                 :email email
                                                 :password password}
                                        :handler (fn [resp]
                                                   (if (= (:status resp) 200)
                                                     (do
                                                       (set! (.-innerHTML signup-btn) "Success!")
                                                       (go (<! (timeout 900))
                                                           (.submit form)))
                                                     (do
                                                       (set! (.-innerHTML signup-btn) signup-btn-text)
                                                       (set! (.-disabled signup-btn) false)
                                                       (cond
                                                         (= (:input resp) "email") (do
                                                                                     (set! (.-innerHTML email-input-help) (:message resp))
                                                                                     (style/setStyle email-input-help "display" "block"))
                                                         :else nil))))
                                        :error-handler handler
                                        :format :json
                                        :response-format :json
                                        :keywords? true}))))))))
