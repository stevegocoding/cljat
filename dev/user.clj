(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            (ring.middleware
             [reload :refer :all]
             [stacktrace :refer :all]
             [webjars :refer :all]
             [defaults :refer :all])
            (server.components
             [http-kit :refer [new-web-server]]
             ;;[jetty :refer [new-web-server]]
             [handler :refer [new-handler]]
             [middleware :refer [new-middleware]]
             [endpoint :refer [new-endpoint]])
            [server.app-routes :refer [new-app-routes]]
            [clj-http.client :as http]
            [figwheel-sidecar.repl-api :refer :all]))

(def sys nil)

(defn system-config
  []
  {:web {:host (env :web-host)
         :port (env :web-port)
         :join? (if (= (env :cljat-env) "development") false true)}})

(defn dev-system
  [sys-config]
  (-> (component/system-map
       :middleware (new-middleware [[wrap-stacktrace]
                                    [wrap-webjars]
                                    [wrap-defaults site-defaults]
                                    [wrap-reload]])
       :routes (new-endpoint new-app-routes)
       :handler (new-handler)
       :web-server (new-web-server (:web sys-config)))
      (component/system-using
       {:handler [:middleware :routes]
        :web-server [:handler]})))

(defn start-sys
  [s]
  (component/start-system s))

(defn stop-sys
  [s]
  (component/stop-system s))

(defn init
  ;; Construct the current development system
  []
  (let [web-config (system-config)]
      (alter-var-root #'sys
                      (constantly (dev-system (system-config))))))

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
