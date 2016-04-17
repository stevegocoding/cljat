(ns cljat-webapp.components.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as s-utils]
            [cljat-webapp.schema :refer :all]
            [clojure.tools.logging :as log]))


(def RedisOptions
  {:host HostName
   :port Port})

(defn RedisOptionsEnv->RedisOptions
  [{:keys [host port] :as options-env}]
  {:host host
   :port port})

(def parse-redis-server-options
  (coerce/coercer RedisOptions {RedisOptions RedisOptionsEnv->RedisOptions
                              Port #(Integer/parseInt %)}))

(defn init-listener [conn]
  (car/with-new-pubsub-listener (:spec conn)
    {}))

(defrecord Redis [options a]
  component/Lifecycle
  
  (start [component]
    (let [host (:host options)
          port (:port options)
          conn {:pool {} :spec {:host host :port port}}]
      (assoc component
             :redis-conn conn
             :listener (init-listener conn))))
  
  (stop [component]
    (assoc component
           :redis-conn nil
           :listener nil)))

(defn new-redis-server [options]
  (log/debug "options: " options)
  (let [coercer parse-redis-server-options]
    (-> (try (s/validate RedisOptions options)
             (catch Exception e (parse-redis-server-options options))) 
      (->Redis options))))

(defn sub-thread [redis tid handler]
  (let [conn (:redis-conn redis)
           listener (:listener redis)
           channel (str "thread@" tid)]
    (car/with-open-listener listener
      (car/subscribe channel))

    (swap! (:state listener) assoc channel handler)))

(defn new-thread [redis tid uids handle-redis]
  (let [conn (:redis-conn redis)
        listener (:listener redis)]
    (car/wcar conn
      (doseq [uid uids]
        (car/sadd (str "thread:" tid) uid)
        (car/with-open-listener listener
          (car/subscribe (str "thread@" tid))
          (swap! (:state listener) assoc (str "thread@" tid) handle-redis))))))

(defn pub-msg [redis tid msg]
  (let [conn (:redis-conn redis)
        channel (str "thread@" tid)]
    (car/wcar conn (car/publish channel msg))))

(defn cache-user-info [redis uid tids handle-redis]
  (let [conn (:redis-conn redis)
        listener (:listener redis)]
    (car/wcar conn
      (doseq [tid tids]
        (car/sadd (str "user:" uid) tid)
        (car/sadd (str "thread:" tid) uid)
        (car/with-open-listener listener
          (car/subscribe (str "thread@" tid))
          (swap! (:state listener) assoc (str "thread@" tid) handle-redis))))))

(defn cleanup-user-info [redis uid]
  (let [conn (:redis-conn redis)]
    (log/debug "cleanup user info in redis")
    (let [tids (car/wcar conn
                 (car/smembers (str "user:" uid)))]
      (car/wcar conn
        (doseq [tid tids]
          (log/debug "cleanup tids: " tid)
          (car/srem (str "thread:" tid) uid))
        (car/del (str "user:" uid))))))

(defn thread-members [redis tid]
  (let [conn (:redis-conn redis)
        thread-key (str "thread:" tid)]
    (car/wcar conn (car/smembers thread-key))))
