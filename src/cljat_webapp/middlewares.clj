(ns cljat-webapp.middlewares
  (:require [ring.util.response :refer [status response content-type not-found redirect set-cookie]]
            [cljat-webapp.auth :refer :all]
            [clojure.tools.logging :as log]))

(defn wrap-auth-token-cookie [handler pubkey]
  (fn [req]
    (let [user-info (when-let [token (-> req :cookies (get "cljat-token") :value)]
                      (try
                        (unsign-token token pubkey)
                        (catch clojure.lang.ExceptionInfo e
                          (do
                            (log/info "Token has expired!")
                            nil))))]
      (if user-info
        (handler (assoc req :identity (:uid user-info)))
        (handler req)))))

(defn wrap-auth-session [handler]
  (fn [req]
    (let [user-info (:session req)]
      (log/debug "wrap-auth-session: " req)
      (if user-info
        (handler (assoc req :identity (:uid user-info)))
        (handler req)))))

(defn wrap-authentication [handler]
  (fn [req]
    (if (:identity req)
      (handler req)
      (redirect "/login"))))
