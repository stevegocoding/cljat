(ns server.components.http-kit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as s-utils]
            [server.schema :refer :all]
            [clojure.tools.logging :as log]))

(def WebOptions
  {:host HostName
   :port Port
   :join? s/Bool})

(defn WebOptionsEnv->WebOptions
  [{:keys [host port join?] :as options-env}]
  {:host host
   :port port
   :join? join?})


(def parse-web-server-options
  (coerce/coercer WebOptions {WebOptions WebOptionsEnv->WebOptions
                              Port #(Integer/parseInt %)}))
 

(defrecord HttpKitServer [options]
  component/Lifecycle
  
  (start [component]
    (let [handler (get-in component [:handler :handler-fn])
          server (run-server handler options)]
      (assoc component :server server)))

  (stop [component]
    (if-let [server (:server component)]
      (server))
    (assoc component :server nil)))

(defn new-web-server
  [options]
  (let [coercer parse-web-server-options]
    (-> (try (s/validate WebOptions options)
             (catch Exception e (parse-web-server-options options))) 
        (->HttpKitServer))))
