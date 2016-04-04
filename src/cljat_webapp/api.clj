(ns cljat-webapp.api
  (:require [compojure.api.sweet :refer [api context GET]]
    [ring.util.http-response :refer [ok]])
  (:import java.lang.String))


(defn api-routes [{:as endpoint-comp}]
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
        (ok {:message (str "Hello, " name)})))))
