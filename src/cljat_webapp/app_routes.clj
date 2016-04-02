(ns cljat-webapp.app-routes
  (:require [clojure.core.async :refer [<! >! put! take! close! thread go go-loop]]
            [clojure.tools.logging :as log]
            [ring.util.response :refer [response content-type]]
            (compojure
             [core :refer [routes GET POST ANY]]
             [route :as route])
            [selmer.parser :as parser]
            [environ.core :refer [env]]))

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
     (parser/render-file "home.html" params)
     (response)
     (content-type "text/html; charset=utf-8"))))

(defn new-app-routes
  [{:keys [ws-handler] :as endpoint-comp}]
  
  ;; set template resource path
  ;; (parser/set-resource-path! (clojure.java.io/resource "templates"))

  ;; return ring handler (route)
  (let [ws-handshake-fn (:ws-handshake-fn ws-handler)]
    (routes (GET "/chat" req (chat-route req))
            (GET "/ws" req (ws-handshake-fn req))
            #_(route/not-found (slurp (io/resource "404.html"))))))
