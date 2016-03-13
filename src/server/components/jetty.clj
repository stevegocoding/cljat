(ns server.components.jetty
  (:import org.eclipse.jetty.server.Server)
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as s-utils]
            [ring.adapter.jetty :refer [run-jetty]]
            [server.schema :refer :all]))

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


(defrecord WebServer [options]
  component/Lifecycle

  (start [component]
    (if (:server component)
      component
      (let [
            handler (atom (delay (get-in component [:handler :handler-fn])))
            server (run-jetty (fn [req] (@@handler req)) options)]
        (assoc component :server server))))
  
  (stop [component]
    (when-let [^Server server (:server component)]
      (do (.stop server)
          (.join server)
          (assoc component :server nil)))))

(defn new-web-server
  [options]
  (let [coercer parse-web-server-options]
    (-> (try (s/validate WebOptions options)
             (catch Exception e (parse-web-server-options options))) 
        (->WebServer))))
