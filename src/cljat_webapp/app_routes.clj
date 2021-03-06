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
   [ring.middleware.session.cookie :refer :all]
   (compojure
     [core :refer [context routes GET POST ANY wrap-routes]]
     [route :as route])
   [clj-http.client :as http]
   [selmer.parser :as parser]
   [environ.core :refer [env]]
   [cljat-webapp.auth :refer [check-user-creds sign-token unsign-token]]
   [cljat-webapp.middlewares :refer [wrap-authentication wrap-auth-token-cookie wrap-auth-session]]
   [cljat-webapp.api :refer [api-routes]]
   [cljat-webapp.model :as m]))

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
      (parser/render-file "chat.html" (assoc params :user-id (:identity req)))
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

(defn signup-page [req]
  (let [params (template-params req {:title "cljat signup"})]
    (->
      (parser/render-file "signup.html" params)
      (response)
      (content-type "text/html; charset=utf-8"))))

(defn do-signup [{:keys [nickname email password] :as params} db]
  (log/debug "nickname: " nickname "email: " email " password: " password)
  (let [row (m/find-user-by-email db email)]
    (if (nil? row)
      (do
        (m/insert-new-user! db nickname email password)
        (->
          (response {:status 200
                     :message "ok"
                     :data {:redirect "/login"}})
          (status 200)
          (content-type "application/json; charset=utf-8")))
      (->
        (response {:status 400
                   :input "email"
                   :message "Email has been registered! Try another one."})
        (status 200)
        (content-type "application/json; charset=utf-8")))))

#_(defn do-login [{:keys [email password] :as params} db privkey]
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

(defn do-login-session [{:keys [email password] :as params} db]
  (let [row (check-user-creds db email password)]
    (log/debug params)
    (log/debug "email: " email " password: " password)
    (if row
      (->
        (response {:message "ok" :data {:redirect "/app/chat"}})
        (assoc :session {:uid (:uid row)})
        (status 200)
        (content-type "application/json; charset=utf-8"))
      (->
        (response {:message "Invalid email or password"})
        (status 401)
        (content-type "application/json; charset=utf-8")))))

(defn get-user-info [user-id db]
  (if-let [user (m/find-user-by-id db user-id)]
    (->
      (response {:data user})
      (status 200)
      (content-type "application/json; charset=utf-8"))
    (->
      (response {:message "User not found"})
      (status 404)
      (content-type "application/json; charset=utf-8"))))

(defn get-user-friends-info [db user-id]
  (if-let [friends (m/find-friends-by-user-id db user-id)]
    (->
      (response {:data friends})
      (status 200)
      (content-type "application/json; charset=utf-8"))
    (->
      (response {:message "User not found"})
      (status 404)
      (content-type "application/json; charset=utf-8"))))

(defn get-user-threads-info [db user-id]
  (let [threads (m/find-threads-by-user-id db user-id)]
    (->
      (response {:data threads})
      (status 200)
      (content-type "application/json; charset=utf-8"))))

(defn add-user-friend [db user-id friend-id]
  (let [result (m/add-friendship db user-id friend-id)]
    (->
      (response {:message "ok"})
      (status 200)
      (content-type "application/json; charset=utf-8")))
  
  #_(->
    (response {:message "ok"})
    (status 200)
    (content-type "application/json; charset=utf-8"))
  )

(defn add-thread [db user-id friend-id]
  (let [result (m/add-thread db user-id friend-id)]
    (->
      (response {:message "ok"
                 :data {:tid result}})
      (status 200)
      (content-type "application/json; charset=utf-8"))))

(defn find-user-by-email [db email]
  (let [user (m/find-user-by-email db email)]
    (->
      (response {:data user})
      (status 200)
      (content-type "application/json; charset=utf-8"))))

(defn find-recent-messages [db user-id]
  (let [result (m/find-newest-messages db user-id)]
    (->
      (response {:data result})
      (status 200)
      (content-type "application/json; charset=utf-8"))))

(defn not-found-route [req]
  (not-found "cljat 404"))

(defn app-routes [{:keys [ws-handler db]}]
  (let [ws-handshake-fn (:ws-handshake-fn ws-handler)]
    (->
      (routes
        (GET "/chat" [] chat-route)
        (GET "/ws" [] ws-handshake-fn)
        (GET "/user-info" req (fn [req]
                                (get-user-info (:identity req) db)))
        (GET "/find-user-by-email" req (fn [req]
                                         (find-user-by-email db (get-in req [:params :user-email]))))
        (GET "/friends-info" req (fn [req]
                                   (get-user-friends-info db (get-in req [:params :user-id]))))
        (POST "/add-friend" req (fn [req]
                                  (add-user-friend db (:identity req) (get-in req [:params :user-id]))))
        (GET "/threads-info" req (fn [req]
                                   (get-user-threads-info db (get-in req [:params :user-id]))))
        (POST "/add-thread" req (fn [req]
                                   (add-thread db
                                     (get-in req [:params :user-id])
                                     (get-in req [:params :friend-id]))))
        (GET "/recent-msgs" req (fn [req]
                                  (find-recent-messages db (get-in req [:params :user-id])))))
      (wrap-stacktrace)
      ;;(wrap-resource "public")
      (wrap-authentication)
      ;;(wrap-auth-token-cookie pubkey)
      (wrap-auth-session)
      (wrap-defaults (assoc site-defaults
                       :security false
                       :session {:store (cookie-store {:key "a 16-byte secret"})}))
      (wrap-json-params)
      (wrap-json-response)
      (wrap-reload))))

(defn site-routes [{:keys [ws-handler db]}]
  (let [wrap-site (fn [handler]
                    (-> handler
                      (wrap-stacktrace)
                      ;;(wrap-session {:cookie-attrs {:secure true}})
                      (wrap-auth-session)
                      (wrap-defaults (assoc site-defaults
                                       :security false
                                       :session {:store (cookie-store {:key "a 16-byte secret"})}))
                      ;;(wrap-cookies)
                      ;;(wrap-keyword-params)
                      (wrap-json-response)
                      (wrap-json-params)
                      (wrap-reload)))]
    (->
      (routes
        (GET "/" [] home-route)
        (GET "/login" req (login-page req))
        (GET "/signup" req (signup-page req))
        (POST "/signup2" req (do-signup (:params req) db))
        (POST "/signup-form" req (redirect "login"))
        (POST "/login" req (do-login-session (:params req) db))
        (POST "/login-form" req (redirect "app/chat")))
      (wrap-routes wrap-site))))

(defn new-app-routes [endpoint]
  (routes
    (route/resources "/")
    (route/resources "/bootstrap/" {:root "META-INF/resources/webjars/bootstrap/3.3.6/"})
    (route/resources "/font-awesome/" {:root "META-INF/resources/webjars/font-awesome/4.5.0/"})
    (site-routes endpoint)
    (context "/api" []
      (api-routes endpoint))
    (context "/app" []
      (app-routes endpoint))
    (route/not-found not-found-route)))
