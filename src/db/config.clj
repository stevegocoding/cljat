(ns db.config
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn h2-db-spec [db-store user password]
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname db-store
   :user user
   :password password})

(defn h2-db-server-spec [db-store user password]
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname (str "tcp://localhost/" db-store ";IFEXISTS=TRUE")
   :user user
   :password password})

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

