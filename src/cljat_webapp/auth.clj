(ns cljat-webapp.auth
  (:require [clojure.tools.logging :as log]
            [buddy.hashers :as hs]
            [buddy.sign.jws :as jws]
            [cljat-webapp.model :as m]
            [clj-time.core :as t]))


(defn check-user-creds [db email password]
  (let [user (m/find-user-by-email db email)]
    (if user
      (if (hs/check password (:password user))
        (dissoc user :password)
        nil))))

(defn sign-token [claims pkey & {:keys [alg exp]
                                 :or {alg :rs256
                                      exp (t/plus (t/now) (t/days 1))}
                                 :as opts}]
  (jws/sign (assoc claims :exp exp) pkey {:alg alg}))

(defn unsign-token [token pubkey & {:keys [alg]
                                    :or {alg :rs256}
                                    :as opts}]
  (log/debug "token " token)
  (log/debug "unsign token -- " pubkey)
  (log/debug "alg: " alg)
  (jws/unsign token pubkey {:alg alg}))
