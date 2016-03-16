(ns cljat-webapp.app)

(enable-console-print!)

(.log js/console "Heymlkajflkjsdfkjflksjadfmmmlaskjflkjf sup?!")

(defn on-message [msg-hist]
  (prn (first msg-hist)))

(defn fig-reload []
  (.log js/console "in fig-reload"))
