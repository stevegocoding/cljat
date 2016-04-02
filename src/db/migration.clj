(ns db.migration
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [db.config :as c]
            [migratus.core :as migratus]))

(def db-store (str (.getAbsolutePath (io/file "db/cljat"))))
(def db-backup (str (.getAbsolutePath (io/file "db/cljat.backup.sql"))))

(def h2-spec (delay (c/h2-db-server-spec db-store "sa" "")))
(def migrate-config {:store :database
                     :migration-dir "migrations"
                     :db @h2-spec})

(defn clear-h2-db []
  (sql/execute! @h2-spec ["DROP ALL OBJECTS"]))

(migratus/migrate migrate-config)
