(ns webapp.core)

(println "hello world - cljat")

(defn on-message [msg-hist]
  (prn (first msg-hist)))

(defn fig-reload []
  (.log js/console "in fig-reload"))
