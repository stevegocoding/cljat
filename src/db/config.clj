(ns db.config)

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
