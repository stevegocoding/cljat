(ns cljat-webapp.components.db
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [com.stuartsierra.component :as component])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn h2-db-spec [{:keys [db-driver db-subprotocol db-subname db-store db-user db-password]}]
  (let [abs-store (str (.getAbsolutePath (io/file db-store)))]
    {:classname db-driver
     :subprotocol db-subprotocol
     :subname (str db-subname abs-store ";IFEXISTS=TRUE")
     :user db-user
     :password db-password}))

(defn pool [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defrecord DB [db-config]
  component/Lifecycle

  (start [component]
    (log/info "Starting DB component ...")
    (let [spec (h2-db-spec db-config)
          pooled (pool spec)]
      (log/debug "db spec" spec)
      (assoc component :conn pooled)))

  (stop [component]
    (assoc component :conn nil)))

(defn new-db [db-config]
  (map->DB {:db-config db-config}))
