(ns user
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            (server.components
             [jetty :refer [new-web-server]]
             ;; [http-kit :refer [new-web-server]]
             [handler :refer [new-handler]])
            [server.app-routes :refer [new-app-routes]]))

(defn middleware-fn
  ([f] f)
  ([f options]
   #(apply f % options)))

(defn compose-middleware
  [middlewares]
  (->> (reverse middlewares)
       (map #(middleware-fn %))
       (apply comp identity)))

(def wrap-mw-fn (compose-middleware [[wrap-session]
                                     [wrap-keyword-params]
                                     [wrap-params]
                                     [wrap-cookies]]))

(def sys nil)

(def sys-config
  {:web {:host "0.0.0.0"
         :port 8080
         :join? false}})

(defn dev-system
  [sys-config]
  (component/system-map
   :handler (new-handler [new-app-routes])
   :web (component/using
         (new-web-server (:web sys-config))
         {:handler :handler})))

(defn start-sys
  [s]
  (component/start-system s))

(defn stop-sys
  [s]
  (component/stop-system s))

(defn init
  ;; Construct the current development system
  []
  (alter-var-root #'sys
                  (constantly (dev-system sys-config))))

(defn start
  ;; Starts the current development system
  []
  (alter-var-root #'sys start-sys))

(defn stop
  ;; Shuts down and destroy the current development system
  []
  (alter-var-root #'sys
                  (fn [s] (when s (stop-sys s)))))

(defn go
  ;; Initialize the current development system
  []
  (init)
  (start))

(defn reset
  ;; Reset the current development system
  []
  (stop)
  (refresh :after 'user/go))
