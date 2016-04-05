(ns cljat-webapp.api
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [api context GET POST]]
            [schema.core :as s]
            [ring.util.http-response :refer [ok created]]
            [buddy.hashers :as hs]
            [buddy.sign.jws :as jws]
            [clj-time.core :as t]
            [cljat-webapp.model :as m])
  (:import java.lang.String))

(s/defschema AuthToken
  {:user-id Long
   :token String})

(defn- check-user-creds [db email password]
  (let [user (m/find-user-by-email db email)]
    (if user
      (if (hs/check password (:password user))
        (dissoc user :password)
        nil))))

(defn- sign-token [claims pkey]
  (let [exp (t/plus (t/now) (t/days 1))]
    (jws/sign (assoc claims :exp exp) pkey {:alg :rs256})))

(defn unsign-token [pubkey]
  (jws/unsign pubkey {:alg :rs256}))

(defn- auth-token-response [db private-key {:keys [email password]}]
  (let [row (check-user-creds db email password)
        token (sign-token row private-key)]
    (if row
      {:status 201 :body {:code 201
                          :message ""
                          :payload {:userid (:user_id row)
                                    :token token}
                          }}
      {:status 401 :body {:code 401
                          :message "Invalid username or password"
                          :payload {}
                          }})))


(defn api-routes [{:keys [db privkey] :as endpoint-comp}]
  (api
    {:swagger {:ui "/swagger-ui"
               :spec "/swagger.json"
               :data {:info {:title "CljatAPI"
                             :description "cljat-webapp api"}
                      :tags [{:name "api" :description "cljat api"}]}}}

    (context "/api" []
      :tags ["api"]
      
      (GET "/hello" []
        :return {:message String}
        :query-params [name :- String]
        (ok {:message (str "Hello, " name)}))

      (POST "/auth-token" req
        :return {:code (s/enum 200 201 400 401 500)
                 :message String
                 :payload AuthToken}
        :body-params [email :- String
                      password :- String]
        (auth-token-response db privkey (:params req))))))
