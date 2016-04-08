(ns cljat-webapp.model
  (:require [clojure.java.jdbc :as sql]))

(defn find-user-by-email [db email]
  (sql/query (:conn db)
    ["select u.user_id as uid, u.email, u.nickname, u.password from users u where u.email = ?" email]
    :result-set-fn first))

(defn find-user-id-by-nickname [conn nickname]
  (sql/query conn ["select user_id from users where nickname = ?" nickname]
    :result-set-fn first))

(defn find-thread-id-by-title [conn title]
  (sql/query conn ["select thread_id from threads where title = ?" title]))

