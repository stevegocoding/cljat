(ns db.seed
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [buddy.hashers :as hashers]
            [buddy.core.nonce :as nonce]
            [db.config :as c]))

(def db-store (str (.getAbsolutePath (io/file "db/cljat"))))
(def h2-spec (delay (c/h2-db-server-spec db-store "sa" "")))

(defn password-hash [password]
  (hashers/derive password {:alg :bcrypt+blake2b-512 :salt (nonce/random-bytes 16)}))

(defn new-user [email nickname pass]
  {:email email
   :nickname nickname
   :password (password-hash pass)})

(defn new-friendship-mutual [db-spec nickname-a nickname-b]
  (let [rs (sql/query db-spec ["select user_id, nickname from users where nickname in (?, ?)" nickname-a nickname-b])
        ids (reduce #(conj %1 (:user_id %2)) '() rs)
        ua (first ids)
        ub (last ids)]
    (conj ()
          {:user_id ua :friend_id ub}
          {:user_id ub :friend_id ua})))

(defn seed-users []
  (let [spec @h2-spec]
    (sql/insert! spec :users (new-user "steve.shi@cljatdemo.com" "steve" "secret"))
    (sql/insert! spec :users (new-user "erika.liang@cljatdemo.com" "erika" "secret"))
    (sql/insert! spec :users (new-user "funny.liang@cljatdemo.com" "funny" "secret"))
    (sql/insert! spec :users (new-user "coco.huang@cljatdemo.com" "coco" "secret"))
    (sql/insert! spec :users (new-user "yoshi@cljatdemo.com" "yoshi" "secret"))
    (sql/insert! spec :users (new-user "woody@cljatdemo.com" "woody" "secret"))))

(defn seed-friendships []
  (let [spec @h2-spec]
    (let [rel (new-friendship-mutual spec "steve" "erika")]
      (sql/insert! spec :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual spec "steve" "funny")]
      (sql/insert! spec :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual spec "erika" "funny")]
      (sql/insert! spec :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual spec "funny" "coco")]
      (sql/insert! spec :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual spec "funny" "woody")]
      (sql/insert! spec :friendships (first rel) (last rel)))
    (let [rel (new-friendship-mutual spec "funny" "yoshi")]
      (sql/insert! spec :friendships (first rel) (last rel)))))
