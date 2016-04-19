(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.repl :refer [doc]]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as sql]
            [environ.core :refer [env]]
            (ring.middleware
             [reload :refer :all]
             [stacktrace :refer :all]
             [webjars :refer :all]
             [defaults :refer :all])
            (cljat-webapp.components
              [http-kit :refer [new-web-server]]
              [db :refer [new-db]]
              [endpoint :refer [new-endpoint]]
              [redis :refer [new-redis-server]]
              [ws-handler :refer [new-ws-handler]]
              [msg-router :refer [new-msg-router]]
              [msg-echo :refer [new-msg-echo]])
            [cljat-webapp.app-routes :refer [new-app-routes]]
            [cljat-webapp.auth :refer :all]
            [cljat-webapp.model :as m]
            [clj-http.client :as http]
            [figwheel-sidecar.repl-api :refer :all]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [buddy.core.keys :as keys]))

(def sys nil)

(defn system-config
  []
  {:web {:host (env :web-host)
         :port (env :web-port)
         :join? (if (= (env :cljat-env) "development") false true)}
   :redis {:host (env :redis-host)
           :port (env :redis-port)}
   :db {:db-subprotocol "h2"
        :db-subname ""
        :db-store "db/cljat"
        :db-driver "org.h2.Driver"
        :db-user "sa"
        :db-password ""}
   :auth {:private-key "auth_privkey.pem"
          :public-key "auth_pubkey.pem"
          :passphrase "secretpassphrase"}})

(defn dev-system
  [sys-config]
  (-> (component/system-map
       ;; The core.async channel from ws handler to message router
       :ws-router-ch (a/chan)

       ;; The core.async channel from message router to ws handler 
       :router-ws-ch (a/chan)

       ;; Middlewares
       ;; :middleware (new-middleware [[wrap-stacktrace] [wrap-webjars] [wrap-defaults site-defaults] [wrap-reload]])
       
       ;; Redis client
       :redis (new-redis-server (:redis sys-config))

       ;; Database
       :db (new-db (:db sys-config))

       ;; A worker that handles the websocket messages to put them on an outgoing channel 
       :ws-handler (new-ws-handler)

       ;; A worker that take a message from an incoming channel, publish to redis and-
       ;; put the response message on an outgoing channel
       :msg-router (new-msg-router)

       ;; A test message echo component
       ;; :msg-echo (new-msg-echo)

       ;; Compojure route
       :routes (new-endpoint (:auth sys-config) new-app-routes)

       ;; Ring handler 
       ;; :handler (new-handler)

       ;; Http-kit server
       :web-server (new-web-server (:web sys-config)))
      
      (component/system-using
       {:ws-handler [:router-ws-ch :ws-router-ch :redis]
        :msg-router {:in-ch :ws-router-ch
                     :out-ch :router-ws-ch
                     :redis :redis}
        ;; :msg-echo {:in-ch :ws-router-ch :out-ch :router-ws-ch}
        
        :routes [:db :ws-handler]
        
        ;; :handler [:middleware :routes]
        ;; :web-server [:handler]
        :web-server [:routes]
        })))

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


#_(m/find-threads-by-user-id (get-in sys [:routes :db]) 1)
#_(r/pub-msg (get-in sys [:redis]) "1" [:cljat/chat-msg {:data {:sent-to "4" :msg-str "from repl channel 4"}}])
