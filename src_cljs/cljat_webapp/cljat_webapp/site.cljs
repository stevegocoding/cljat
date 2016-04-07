(ns cljat-webapp.site
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <!]]
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

(let [login-ch (listen (dom/getElement "login-btn") goog.events.EventType.CLICK)]
  (go (while true
        (<! login-ch)
        (let [email (.-value (dom/getElement "email-input"))
              password (.-value (dom/getElement "password-input"))]
          #_(GET "/api/hello" {:params {:name "steve"}
                             :format :json
                             :response-format :json
                             :keywords? true})
          (POST "/login" {:params {:name "alkdfjlkajf"
                                   }
                          #_{:email email
                                   :password password}
                          :handler handler
                          :error-handler handler
                          :format :json
                          :response-format :json
                          :keywords? true})))))
