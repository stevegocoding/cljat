(ns cljat-webapp.model
  (:require [clojure.java.jdbc :as sql]))

(defn find-user-by-email [db email]
  (sql/query (:conn db)
    ["select u.user_id as uid, u.email, u.nickname, u.password from users u where u.email = ?" email]
    :result-set-fn first))

(defn find-user-by-id [db id]
  (sql/query (:conn db)
    ["select u.user_id as uid, u.email, u.nickname from users u where u.user_id = ?" id]
    :result-set-fn first))

(defn find-friends-by-user-id [db user-id]
  (sql/query (:conn db)
    [(str "select fs.user_id as uid, fs.email, fs.nickname from users fs where fs.user_id in "
       "(" "select f.friend_id from users u join friendships f on u.user_id = f.user_id where u.user_id = ?" ")") user-id]))

(defn find-user-id-by-nickname [conn nickname]
  (sql/query conn ["select user_id from users where nickname = ?" nickname]
    :result-set-fn first))

(defn find-thread-id-by-title [conn title]
  (sql/query conn ["select thread_id from threads where title = ?" title]))

