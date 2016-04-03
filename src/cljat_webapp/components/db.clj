(ns cljat-webapp.components.db
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [com.stuartsierra.component :as component]))

(defrecord DB [config db-spec]
  component/Lifecycle

  (start [component]
    
    )

  (stop [component]
    )
  )

(defn new-db [config]
  (map->DB {:config config}))
