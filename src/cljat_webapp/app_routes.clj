(ns cljat-webapp.app-routes
  (:require
    [clojure.core.async :refer [<! >! put! take! close! thread go go-loop]]
    [clojure.tools.logging :as log]
    [ring.util.response :refer [response content-type not-found]]
    [ring.util.request :refer [body-string]]
    (ring.middleware
      [reload :refer :all]
      [stacktrace :refer :all]
      [webjars :refer :all]
      [defaults :refer :all]
      [keyword-params :refer :all]
      [params :refer :all]
      [resource :refer :all])
    [ring.middleware.json :refer :all]
    (compojure
      [core :refer [context routes GET POST ANY]]
      [route :as route])
    [clj-http.client :as http]
    [selmer.parser :as parser]
    [environ.core :refer [env]]
    [cljat-webapp.api :refer [check-user-creds
                              sign-token
                              api-routes]]))

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
        {:status 200
         :headers {}
         :body {:message "ok"
                :data {:redirect "/chat"}}
         :cookies {"cljat_token" {:value token}}})
      {:status 401
       :headers {}
       :body {:message "Invalid email or password"
              :data {}}})))

(defn not-found-route [req]
  (not-found "cljat 404"))

(defn site-routes [{:keys [ws-handler db privkey]}]
  (let [ws-handshake-fn (:ws-handshake-fn ws-handler)]
    (->
      (routes
        (GET "/" [] home-route)
        (GET "/login" [] (fn [req] (login-page req)))
        (POST "/login" req (fn [req]
                             (log/debug "--- " req)
                             (-> (response "Hello World")
                               (content-type "text/plain"))
                             #_(do-login (:params req) db privkey)
                             ))
        (GET "/chat" [] chat-route)
        (GET "/ws" [] ws-handshake-fn)
        (route/not-found not-found-route))
      (wrap-stacktrace)
      (wrap-webjars)
      ;;(wrap-resource "public")
      (wrap-defaults (assoc site-defaults :security false))
      ;;(wrap-keyword-params)
      ;;(wrap-params)
      (wrap-json-params)
      (wrap-reload))))

(defn new-app-routes [endpoint]
  #_(site-routes endpoint)
  #_(routes
    (api-routes endpoint)
    (site-routes endpoint)))
