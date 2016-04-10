(ns cljat-webapp.api
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [api context GET POST]]
            [schema.core :as s]
            [ring.util.http-response :refer [ok created]]
            (ring.middleware
              [cookies :refer :all]
              [session :refer :all]
              [reload :refer :all]
              [stacktrace :refer :all]
              [webjars :refer :all]
              [defaults :refer :all]
              [keyword-params :refer :all]
              [params :refer :all]
              [resource :refer :all]
              [json :refer :all])
            [clj-time.core :as t]
            [cljat-webapp.auth :refer :all]
            [cljat-webapp.model :as m])
  (:import java.lang.String))

(s/defschema AuthToken
  {:user-id Long
   :token String})

(s/defschema UserInfo
  {:user-id Long
   :email String
   :nickname String})

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

(defn- get-user-info-resp [db {:keys [user-id]}]
  (if-let [user (m/find-user-by-id user-id)]
    {:status 200
     :body {:code 200
            :payload {:user-id (:uid user)
                      :email (:email user)
                      :nickname (:nickname user)}}}
    {:status 404
     :body {:code 404
            :message "User not found!"}}))


(defn api-routes [{:keys [db privkey pubkey] :as endpoint-comp}]
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

    #_(GET "/user-info" req
      :return {:code (s/enum 200 201 400 401 500)
               :message String
               :payload UserInfo}
      (get-user-info-resp db (:identity req)))

    #_(GET "/friends" req
        :return {:code (s/enum 200 201 400 401 500)
                 :message String
                 :payload FriendsList}
        :query-params [user-id :- Long]
        (log/debug "get friends list -- " req)
        (get-friends-list-resp db (:params req))))
  )
