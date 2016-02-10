(defproject webapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ["-Xmx1g"]
  
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.7.228"]
                 ;; [org.clojure/tools.namespace "0.2.11"]

                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 ;; environ
                 [environ "1.0.2"]

                 ;; Aleph async server
                 [aleph "0.4.1-beta2"]

                 ;; redis client
                 [com.taoensso/carmine "2.12.2"]
                 
                 [ring/ring-core "1.4.0"]
                 [compojure "1.4.0"]

                 ;; component framework
                 [com.stuartsierra/component "0.3.1"]]
  
  :plugins [[lein-pprint "1.1.1"]
            [lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-6"]
            [lein-environ "1.0.2"]]

  :source-paths ["src", "src-cljs"]

  ;; Entry point
  ;; :main ^:skip-aot server.main

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/js/app.js"
                                   :optimizations :none
                                   :pretty-print true}}]}

  :figwheel {:http-server-root "public"
             :port 3449
             :css-dirs ["resources/public/css"]}

  :profiles {
             :default [:base :system :user :provided :dev :dev-env]
             ;; Activated by default
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.nrepl "0.2.12"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/java.classpath "0.2.3"]
                                  [ring/ring-devel "1.4.0"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-4"]]

                   :plugins [[cider/cider-nrepl "0.11.0-SNAPSHOT"]
                             [refactor-nrepl "2.0.0-SNAPSHOT"]]

                   :repl-options {:nrepl-middleware [cider.nrepl.middleware.pprint/wrap-pprint
                                                     cider.nrepl.middleware.pprint/wrap-pprint-fn
                                                     cemerick.piggieback/wrap-cljs-repl]
                                  :host "0.0.0.0"}

                   :ring {:nrepl {:start? true
                                  :host "0.0.0.0"
                                  :port 7888}}}

             ;; Activated by uberjar task
             :uberjar {:aot :all}
             
             ;; Test Profile
             :test {}

             ;; Production Profile
             :prod {:open-browser? false
                    :stacktracers? false
                    :auto-reload? false}}
  )
