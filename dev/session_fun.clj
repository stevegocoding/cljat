(ns session-fun
  (:require [com.stuartsierra.component :as component]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            (ring.middleware
             [reload :refer :all]
             [session :refer :all]
             [stacktrace :refer :all]
             [webjars :refer :all])
            [ring.util.response :refer :all]
            [ring.middleware.session.cookie :refer :all]
            [compojure.route :as route]
            [compojure.core :refer [routes GET POST ANY]]
            [selmer.parser :as parser]
            (cljat-webapp.components
             [http-kit :refer [new-web-server]]
             ;;[jetty :refer [new-web-server]]
             [handler :refer [new-handler]]
             [middleware :refer [new-middleware]]
             [endpoint :refer [new-endpoint]])
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.ring :refer [carmine-store]]))

(defn html-escape [string]
  (str "<pre>" (clojure.string/escape string {\< "&lt;", \> "&gt;"}) "</pre>"))

(defn format-request [name request kill-keys kill-headers]
  (let [r1 (reduce dissoc request kill-keys)
        r (reduce (fn [h n] (update-in h [:headers] dissoc n)) r1 kill-headers)]
  (with-out-str
    (println "-------------------------------")
    (println name)
    (println "-------------------------------")
    (clojure.pprint/pprint r)
    (println "-------------------------------"))))


(def kill-keys [:body :request-method :character-encoding :remote-addr :server-name :server-port :ssl-client-cert :scheme  :content-type  :content-length])
(def kill-headers ["user-agent" "accept" "accept-encoding" "accept-language" "accept-charset" "connection" "host"])

(defn wrap-spy [handler spyname]
  (fn [request]
    (let [incoming (format-request (str spyname ":\n Incoming Request:") request kill-keys kill-headers)]
      (println incoming)
      (let [response (handler request)]
        (let [outgoing (format-request (str spyname ":\n Outgoing Response Map:") response kill-keys kill-headers)]
          (println outgoing)
          (update-in response  [:body] (fn[x] (str (html-escape incoming) x  (html-escape outgoing)))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def redis-conn {:pool {} :sepc {:host "192.168.99.100" :port 6379}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def sys nil)

(defn system-config
  []
  {:web {:host (env :web-host)
         :port (env :web-port)
         :join? (if (= (env :cljat-env) "development") false true)}})

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (let [s (:session request)]
           (if (empty? s)
             (str "<h1>Hello World!</h1>")
             (str "<h1>Your Session</h1><p>" s "</p>")))
   :session "I am a session. Fear hahahag"})


(parser/set-resource-path! (clojure.java.io/resource "templates"))

(defn session-template [request]
  (let [count ((request :session {}) :count 0)]
    (-> (response (parser/render-file "session.html" {:session-status (if (zero? count)
                                                                        "Hello World"
                                                                        (str "Your count is: " count))}))
        (assoc :session {:count (inc count)}))))

(defn new-app-routes
  [h]
  (routes (GET "/" request (handler request))
          (GET "/template" request (session-template request))))

(defn dev-system
  [sys-config]
  (-> (component/system-map
       :middleware (new-middleware [[wrap-stacktrace]
                                    ;;[wrap-spy "what the handler sees"]
                                    [wrap-session {:store (carmine-store redis-conn)}]
                                    ;;[wrap-spy "what the web server sees"]
                                    ])
       :routes (new-endpoint new-app-routes)
       :handler (new-handler)
       :web-server (new-web-server (:web sys-config)))
      (component/system-using
       {:handler [:middleware :routes]
        :web-server [:handler]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
  (refresh :after 'session-fun/go))
