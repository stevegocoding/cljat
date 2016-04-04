(ns cljat-webapp.components.http-kit
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as s-utils]
            [cljat-webapp.schema :refer :all]))

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
    (log/info "Starting http-kit server ...")
    (let [handler (get-in component [:routes :handler])
          server (run-server handler options)]
      (assoc component :server server)))

  (stop [component]
    (log/info "Stopping http-kit server ...")
    (if-let [server (:server component)]
      (server))
    (assoc component :server nil)))

(defn new-web-server
  [options]
  (let [coercer parse-web-server-options]
    (-> (try (s/validate WebOptions options)
             (catch Exception e (parse-web-server-options options))) 
        (->HttpKitServer))))
