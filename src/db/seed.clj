(ns db.seed
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [buddy.hashers :as hashers]
            [buddy.core.nonce :as nonce]
            [db.config :as c]))

(def db-store (str (.getAbsolutePath (io/file "db/cljat"))))
(def db-pooled (delay (c/pool (c/h2-db-server-spec db-store "sa" ""))))

(defn db-conn [] @db-pooled)

(defn password-hash [password]
  (hashers/derive password {:alg :bcrypt+blake2b-512 :salt (nonce/random-bytes 16)}))

(defn new-user [email nickname pass]
  {:email email
   :nickname nickname
   :password (password-hash pass)})

(defn new-friendship-mutual [conn nickname-a nickname-b]
  (let [rs (sql/query conn ["select user_id, nickname from users where nickname in (?, ?)" nickname-a nickname-b])
        ids (reduce #(conj %1 (:user_id %2)) '() rs)
        ua (first ids)
        ub (last ids)]
    (conj ()
          {:user_id ua :friend_id ub}
          {:user_id ub :friend_id ua})))

(defn new-thread [title]
  {:title title
   :created_time (tc/to-sql-time (t/now))})

(defn new-message [from to content]
  {:sender_id from
   :dest_id to
   :content content
   :timestamp (tc/to-sql-time (t/now))})

(defn find-user-id-by-nickname [conn nickname]
  (sql/query conn ["select user_id from users where nickname = ?" nickname]
             :result-set-fn first))

(defn find-thread-id-by-title [conn title]
  (sql/query conn ["select thread_id from threads where title = ?" title]
             :result-set-fn first))

(defn new-users-threads [conn title users]
  (let [tid (:thread_id (find-thread-id-by-title conn title))]
    (reduce #(conj %1 [tid (:user_id (find-user-id-by-nickname conn %2))]) [] users)))

(defn seed-users []
  (let [conn (db-conn)]
    (sql/insert! conn :users (new-user "steve.shi@gmail.com" "steve" "secret"))
    (sql/insert! conn :users (new-user "erika.liang@gmail.com" "erika" "secret"))
    (sql/insert! conn :users (new-user "funny.liang@gmail.com" "funny" "secret"))
    (sql/insert! conn :users (new-user "coco.huang@gmail.com" "coco" "secret"))
    (sql/insert! conn :users (new-user "yoshi@gmail.com" "yoshi" "secret"))
    (sql/insert! conn :users (new-user "woody@gmail.com" "woody" "secret"))))

(defn seed-friendships []
  (let [conn (db-conn)]
    (let [rel (new-friendship-mutual conn "steve" "erika")]
      (sql/insert! conn :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual conn "steve" "funny")]
      (sql/insert! conn :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual conn "erika" "funny")]
      (sql/insert! conn :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual conn "funny" "coco")]
      (sql/insert! conn :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual conn "funny" "woody")]
      (sql/insert! conn :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual conn "funny" "yoshi")]
      (sql/insert! conn :friendships (first rel) (last rel)))))

(defn seed-threads []
  (let [conn (db-conn)]
    (sql/insert! conn :threads (new-thread "baba mama & funny"))
    (sql/insert! conn :threads (new-thread "corgi group"))
    (sql/insert! conn :threads (new-thread "thread-1"))
    (sql/insert! conn :threads (new-thread "thread-2"))
    (sql/insert! conn :threads (new-thread "thread-3"))))

(defn seed-users-threads []
  (let [conn (db-conn)]
    (let [rel (new-users-threads conn "baba mama & funny" ["steve" "erika" "funny"])]
      (doseq [r rel]
        (sql/insert! conn :users_threads {:thread_id (r 0) :user_id (r 1)})))
    (let [rel (new-users-threads conn "corgi group" ["funny" "coco" "woody" "yoshi"])]
      (doseq [r rel]
        (sql/insert! conn :users_threads {:thread_id (r 0) :user_id (r 1)})))
    (let [rel (new-users-threads conn "thread-1" ["funny" "coco"])]
      (doseq [r rel]
        (sql/insert! conn :users_threads {:thread_id (r 0) :user_id (r 1)})))
    (let [rel (new-users-threads conn "thread-2" ["funny" "steve"])]
      (doseq [r rel]
        (sql/insert! conn :users_threads {:thread_id (r 0) :user_id (r 1)})))
    (let [rel (new-users-threads conn "thread-3" ["funny" "erika"])]
      (doseq [r rel]
        (sql/insert! conn :users_threads {:thread_id (r 0) :user_id (r 1)})))))

(defn seed-messages []
  (let [conn (db-conn)
        tid-1 (:thread_id (find-thread-id-by-title conn "thread-1"))
        tid-2 (:thread_id (find-thread-id-by-title conn "thread-2"))
        tid-3 (:thread_id (find-thread-id-by-title conn "baba mama & funny"))
        uid-1 (:user_id (find-user-id-by-nickname conn "funny"))
        uid-2 (:user_id (find-user-id-by-nickname conn "steve"))
        uid-3 (:user_id (find-user-id-by-nickname conn "coco"))]
    (sql/insert! conn :messages (new-message uid-1 tid-3 "from funny - hi baba mama"))
    (sql/insert! conn :messages (new-message uid-2 tid-2 "from steve - hi funny, this is baba"))
    (sql/insert! conn :messages (new-message uid-3 tid-1 "from coco - hi funny, this is coco"))))

(defn seed-all []
  (do
    (seed-users)
    (seed-friendships)
    (seed-threads)
    (seed-users-threads)
    (seed-messages)))
