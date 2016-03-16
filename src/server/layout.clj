(ns server.layout
  (:require [selmer.parser :as parser]
            [compojure.response :refer [Renderable]]
            [ring.util.response :refer [content-type response]]
            [environ.core :refer [env]]))

(parser/set-resource-path! (clojure.java.io/resource "templates"))

(deftype RenderableTemplate [template params]
  Renderable
  (render [this request]
    (content-type
     (->> (assoc params
                 :dev (= (env :cljat-env) "development")
                 :servlet-context (if-let [context (:servlet-context request)]
                                    (.getContextPath context)))
          (parser/render-file template)
          response)
     "text/html; charset=utf-8")))

(defn render
  [template & [params]]
  (RenderableTemplate. template params))

