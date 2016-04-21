(ns cljat-webapp.model
  (:require [clojure.java.jdbc :as sql]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]))

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

(defn add-friendship [db user-id friend-id]
  (sql/insert! (:conn db) :friendships {:user_id user-id :friend_id friend-id} {:user_id friend-id :friend_id user-id}))

(defn add-thread [db user-id friend-id]
  (sql/with-db-transaction [conn (:conn db)]
    (let [new-thread-id (->
                          (sql/insert! conn :threads {:title "chat default" :created_time (tc/to-sql-time (t/now))})
                          (first) (vals) (first))]
      (sql/insert! conn :users_threads {:user_id user-id :thread_id new-thread-id})
      (sql/insert! conn :users_threads {:user_id friend-id :thread_id new-thread-id})
      new-thread-id)))

(defn find-threads-by-user-id [db user-id]
  "Example: select u.user_id as uid, u.nickname, ut.thread_id as tid, t.title, t.created_time from users u 
        inner join users_threads ut on u.user_id = ut.user_id 
        inner join threads t on ut.thread_id = t.thread_id 
        where ut.thread_id in 
        (select ut2.thread_id from users_threads ut2 inner join users u2 on ut2.user_id = u2.user_id where u2.user_id = 4)
        and u.user_id != 4"
  (sql/query (:conn db)
    [(str
       "select u.user_id as uid, u.nickname, ut.thread_id as tid, t.title, t.created_time from users u " 
       " inner join users_threads ut on u.user_id = ut.user_id " 
       " inner join threads t on ut.thread_id = t.thread_id "
       " where ut.thread_id in "
       " ("
       " select ut2.thread_id from users_threads ut2 inner join users u2 on ut2.user_id = u2.user_id where u2.user_id = ?"
       " )"
       " and u.user_id != ?") user-id user-id]
    :result-set-fn (fn [rs]
                     (->
                       (reduce (fn [col x]
                                 (if-let [t (get col (:tid x))]
                                   (update-in col [(:tid x) :users] conj (:uid x))
                                   (assoc col (:tid x) {:tid (:tid x)
                                                        :title (:title x)
                                                        :users [(:uid x)]}))) {} rs)
                       (vals)))))


(defn find-user-id-by-nickname [conn nickname]
  (sql/query conn ["select user_id from users where nickname = ?" nickname]
    :result-set-fn first))

(defn find-thread-id-by-title [conn title]
  (sql/query conn ["select thread_id from threads where title = ?" title]))

