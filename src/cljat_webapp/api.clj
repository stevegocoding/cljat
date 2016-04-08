(ns cljat-webapp.api
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [api context GET POST]]
            [schema.core :as s]
            [ring.util.http-response :refer [ok created]]
            [clj-time.core :as t]
            [cljat-webapp.auth :refer :all])
  (:import java.lang.String))

(s/defschema AuthToken
  {:user-id Long
   :token String})

(defn- auth-token-response [db private-key {:keys [email password]}]
  (let [row (check-user-creds db email password)
        token (sign-token row private-key)]
    (if row
      {:status 201 :body {:code 201
                          :message ""
                          :payload {:userid (:uid row)
                                    :token token}
                          }}
      {:status 401 :body {:code 401
                          :message "Invalid username or password"
                          :payload {}
                          }})))

(defn- get-friends-list-resp [db {:keys [user-id]}])


(defn api-routes [{:keys [db privkey] :as endpoint-comp}]
  (api
    {:swagger {:ui "/swagger-ui"
               :spec "/swagger.json"
               :data {:info {:title "CljatAPI"
                             :description "cljat-webapp api"}
                      :tags [{:name "api" :description "cljat api"}]}}}

    (GET "/hello" req
      :return {:message String}
      :query-params [name :- String]
      (log/debug "hello -- " req)
      (ok {:message (str "Hello, " name)}))

    (POST "/auth-token" req
      :return {:code (s/enum 200 201 400 401 500)
               :message String
               :payload AuthToken}
      :body-params [email :- String
                    password :- String]
      (auth-token-response db privkey (:params req)))

    #_(GET "/friends" req
      :return {:code (s/enum 200 201 400 401 500)
               :message String
               :payload FriendsList}
      :query-params [user-id :- Long]
      (log/debug "get friends list -- " req)
      (get-friends-list-resp db (:params req)))))
