(ns cljat-webapp.schema
  (:require [schema.core :as s]))

(def Port (s/both s/Int (s/pred #(<= 0 % 65535))))

(def IpAddress s/Str)

(def HostName s/Str)
