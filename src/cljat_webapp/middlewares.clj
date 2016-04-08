(ns cljat-webapp.middlewares
  (:require [ring.util.response :refer [status response content-type not-found redirect set-cookie]]
            [cljat-webapp.auth :refer :all]))

(defn wrap-auth-token-cookie [handler pubkey]
  (fn [req]
    (let [user-info (when-let [token (-> req :cookies (get "cljat-token") :value)]
                      (unsign-token token pubkey))]
      (handler (assoc req :identity (:uid user-info))))))

(defn wrap-authentication [handler]
  (fn [req]
    (if (:identity req)
      (handler req)
      (redirect "/login"))))
