(ns cljat-webapp.app-routes
  (:require
    [clojure.core.async :refer [<! >! put! take! close! thread go go-loop]]
    [clojure.tools.logging :as log]
    [ring.util.response :refer [status response content-type not-found redirect set-cookie]]
    [ring.util.request :refer [body-string]]
    (ring.middleware
      [cookies :refer :all]
      [session :refer :all]
      [reload :refer :all]
      [stacktrace :refer :all]
      [webjars :refer :all]
      [defaults :refer :all]
      [keyword-params :refer :all]
      [params :refer :all]
      [resource :refer :all])
    [ring.middleware.json :refer :all]
    (compojure
      [core :refer [context routes GET POST ANY wrap-routes]]
      [route :as route])
    [clj-http.client :as http]
    [selmer.parser :as parser]
    [environ.core :refer [env]]
    [cljat-webapp.auth :refer [check-user-creds sign-token unsign-token]]
    [cljat-webapp.middlewares :refer [wrap-authentication wrap-auth-token-cookie]]
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

(defn login-page [req]
  (let [params (template-params req {:title "cljat login"})]
    (->
      (parser/render-file "login.html" params)
      (response)
      (content-type "text/html; charset=utf-8"))))

(defn do-login[{:keys [email password] :as params} db privkey]
  (let [row (check-user-creds db email password)]
    (log/debug params)
    (log/debug "email: " email " password: " password)
    (if row
      (let [token (sign-token row privkey)]
        (->
          (response {:message "ok" :data {:redirect "/app/chat"}})
          (status 200)
          (content-type "application/json; charset=utf-8")
          (set-cookie "cljat-token" token)))
      (->
          (response {:message "Invalid email or password"})
          (status 401)
          (content-type "application/json; charset=utf-8")))))

(defn not-found-route [req]
  (not-found "cljat 404"))

(defn app-routes [{:keys [ws-handler db privkey pubkey]}]
  (let [ws-handshake-fn (:ws-handshake-fn ws-handler)]
    (->
      (routes
        (GET "/chat" [] chat-route)
        (GET "/ws" [] ws-handshake-fn))
      (wrap-stacktrace)
      ;;(wrap-resource "public")
      (wrap-authentication)
      (wrap-auth-token-cookie pubkey)
      (wrap-defaults (assoc site-defaults :security false))
      ;;(wrap-keyword-params)
      ;;(wrap-params)
      (wrap-json-params)
      (wrap-json-response)
      (wrap-reload))))

(defn site-routes [{:keys [ws-handler db privkey pubkey]}]
  (let [wrap-site (fn [handler]
                    (-> handler
                      (wrap-stacktrace)
                      ;;(wrap-session)
                      ;;(wrap-defaults (assoc site-defaults :security false))
                      (wrap-cookies)
                      (wrap-keyword-params)
                      (wrap-json-response)
                      (wrap-json-params)
                      (wrap-reload)))]
    (->
      (routes
        (GET "/" [] home-route)
        (GET "/login" req (fn [req]
                            (do 
                              (log/debug "login --- " req)
                              (login-page req))))
        (POST "/login" req (fn [req]
                             (log/debug "--- " req)
                             #_(-> (response "Hello World")
                                 (content-type "text/plain"))
                             (do-login (:params req) db privkey)))
        (POST "/login-form" req (fn [req]
                                  (redirect "app/chat"))))
      (wrap-routes wrap-site))))

(defn new-app-routes [endpoint]
  (routes
    (route/resources "/")
    (route/resources "/bootstrap/" {:root "META-INF/resources/webjars/bootstrap/3.3.6/"})
    (site-routes endpoint)
    (context "/api" []
      (api-routes endpoint))
    (context "/app" []
      (app-routes endpoint))
    (route/not-found not-found-route)))
