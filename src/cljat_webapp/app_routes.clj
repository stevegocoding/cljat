(ns cljat-webapp.app-routes
  (:require
    [clojure.core.async :refer [<! >! put! take! close! thread go go-loop]]
    [clojure.tools.logging :as log]
    [ring.util.response :refer [response content-type not-found]]
    (ring.middleware
      [reload :refer :all]
      [stacktrace :refer :all]
      [webjars :refer :all]
      [defaults :refer :all])
    (compojure
      [core :refer [context routes GET POST ANY]]
      [route :as route])
    [clj-http.client :as http]
    [selmer.parser :as parser]
    [environ.core :refer [env]]
    [cljat-webapp.api :refer [api-routes]]))

;; App Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn template-params [req params]
  (assoc params
    :dev (= (env :cljat-env) "development")
    :servlet-context (when-let [context (:servlet-context req)]
                       (.getContextPath context))))

(defn chat-route [req]
  (let [params (template-params req {:home-title "cljat chat"})]
    (->
     (parser/render-file "chat.html" params)
     (response)
     (content-type "text/html; charset=utf-8"))))

(defn home-route [req]
  (let [params (template-params req {:title "cljat home"})]
    (->
      (parser/render-file "home.html" params)
      (response)
      (content-type "text/html; charset=utf-8"))))

(defn login-route [req]
  (let [params (template-params req {:title "cljat login"})]
    (->
      (parser/render-file "login.html" params)
      (response)
      (content-type "text/html; charset=utf-8"))))

(defn do-login-route [req]
  )

(defn not-found-route [req]
  (not-found "cljat 404"))

(defn site-routes [{:keys [ws-handler]}]
  (let [ws-handshake-fn (:ws-handshake-fn ws-handler)]
    (context "/site" []
      (->
        (routes
          (GET "/" [] home-route)
          (GET "/login" [] login-route)
          (POST "/login" [] do-login-route)
          (GET "/chat" [] chat-route)
          (GET "/ws" [] ws-handshake-fn)
          (route/not-found not-found-route))
        (wrap-stacktrace)
        (wrap-webjars)
        (wrap-defaults site-defaults)
        (wrap-reload)))))

(defn new-app-routes [endpoint]
  (routes
    (api-routes endpoint)
    (site-routes endpoint)))
