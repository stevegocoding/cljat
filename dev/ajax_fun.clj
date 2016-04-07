(ns ajax-fun
  (:require [com.stuartsierra.component :as component]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [selmer.parser :as parser]
            [hiccup.core :as hiccup]
            [hiccup.page :refer [include-js include-css]]
            [hiccup.element :refer [link-to]]
            [ring.util.response :refer [response content-type not-found]]
            (ring.middleware
             [reload :refer :all]
             [stacktrace :refer :all]
             [webjars :refer :all]
             [keyword-params :refer :all]
             [params :refer :all]
             [resource :refer :all]
             [defaults :refer :all]
             [json :refer :all])
            (compojure
              [core :refer [context routes GET POST ANY]]
              [route :as route])
            (cljat-webapp.components
              [http-kit :refer [new-web-server]]
              [endpoint :refer [new-endpoint]])
            [clj-http.client :as http]
            [figwheel-sidecar.repl-api :refer :all]))

(def sys nil)

(defn system-config
  []
  {:web {:host (env :web-host)
         :port (env :web-port)
         :join? (if (= (env :cljat-env) "development") false true)}
   :redis {:host (env :redis-host)
           :port (env :redis-port)}
   :auth {:private-key "auth_privkey.pem"
          :public-key "auth_pubkey.pem"
          :passphrase "secretpassphrase"}})

(defn not-found-route [req]
  (not-found "cljat 404"))

(defn ajaxfun-page [req]
  (content-type (->
                  (hiccup/html
                    [:html
                     [:head
                      (include-css "/assets/bootstrap/css/bootstrap.min.css")
                      ]
                     [:body
                      [:h1 {:class "title"} "Welcome to ajax fun"]
                      [:div
                       (link-to {:id "ajax-btn" :class "btn btn-primary"} "/doajax" "do ajax post")]
                      (include-js "js/ajax_fun/ajaxfun.js")]])
                  (response))
    "text/html; charset=utf-8"))

(defn do-ajax [req]
  (log/debug req)
  (-> (response "Hello World")
    (content-type "text/plain")))

(defn new-app-routes [{:as endpoint}]
  (->
    (routes
      (GET "/ajaxfun" req (ajaxfun-page req))
      (POST "/doajax" req (fn [req] (do-ajax req)))
      (route/not-found not-found-route))
    (wrap-stacktrace)
    (wrap-webjars)
    (wrap-resource "public")
    (wrap-defaults (assoc site-defaults :security false))
    ;;(wrap-keyword-params)
    ;;(wrap-params)
    (wrap-json-params)
    (wrap-reload)))

(defn dev-system
  [sys-config]
  (-> (component/system-map
       :routes (new-endpoint (:auth sys-config) new-app-routes)
       :web-server (new-web-server (:web sys-config)))
      
      (component/system-using
       {:web-server [:routes]})))

(defn start-sys
  [s]
  (component/start-system s))

(defn stop-sys
  [s]
  (component/stop-system s))

(defn init []
  (let [web-config (system-config)]
      (alter-var-root #'sys
                      (constantly (dev-system (system-config))))))

(defn start []
  (alter-var-root #'sys start-sys))

(defn stop []
  (alter-var-root #'sys
                  (fn [s] (when s (stop-sys s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'ajax-fun/go))
