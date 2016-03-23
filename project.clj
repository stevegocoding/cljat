(defproject webapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ["-Xmx2g" "-XX:MaxPermSize=1G"  "-server"]
  
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.7.228"]

                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 ;; component framework
                 [com.stuartsierra/component "0.3.1"]

                 ;; schema
                 [prismatic/schema "1.0.5"]

                 ;; environ
                 [environ "1.0.2"]

                 ;; http-kit server
                 [http-kit "2.1.18"]

                 ;; redis client
                 [com.taoensso/carmine "2.12.2"]

                 [ring/ring "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-headers "0.1.3"]
                 [ring/ring-mock "0.3.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-webjars "0.1.1"]
                 [compojure "1.4.0"]
                 [clj-http "2.1.0"]
                 [selmer "1.0.2"]

                 ;; Front-end stuff
                 [org.webjars/bootstrap "3.3.6"]
                 [org.webjars/font-awesome "4.5.0"]
                 [cljsjs/bootstrap "3.3.6-0"]
                 [cljsjs/react-bootstrap "0.28.1-1" :exclusions [org.webjars.bower/jquery]]
                 [jayq "2.5.4"]
                 [reagent "0.6.0-alpha"]]
  
  :plugins [[lein-pprint "1.1.1"]
            [lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-6"]
            [lein-environ "1.0.2"]]

  ;; Clojurescript compiler configs
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src_cljs"]
                        :figwheel {:websocket-host "localhost"
                                   :on-jsload cljat-webapp.app/fig-reload}
                        :compiler {
                                   :main "cljat-webapp.app"
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :pretty-print true}}]}

  :figwheel {:http-server-root "public"
             :server-ip "0.0.0.0"
             :server-port 8081
             
             ;; CSS reloading
             :css-dirs ["resources/public/css"]

             ;; nRepl
             :nrepl-port 7889
             :nrepl false
             :hawk-options {:watcher :polling}

             :server-logfile "log/figwheel_server.log" 
             }


;;; File System Paths
  :source-paths ["src", "src_cljs"]

  ;; profile-isolated target paths
  :target-path "target/%s/"

;;; Entry Point
  :main server.main
  :nrepl-options {:init-ns user}
  
  :profiles {
             :dev [:project-dev :local-dev]
             :test {:env {:cljat-env "test"}}
             :prod {:env {:cljat-env "production"}}
             :project-dev {:source-paths ["dev"]
                           :dependencies [[org.clojure/java.classpath "0.2.3"]
                                          [ring/ring-devel "1.4.0"]
                                          [javax.servlet/servlet-api "2.5"]
                                          [com.cemerick/piggieback "0.2.1"]
                                          [figwheel-sidecar "0.5.0-4"]]

                           :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                           
                           :host "0.0.0.0"
                           :env {:cljat-env "development"}}
             
             ;; be overriden by the values from  profiles.clj
             :local-dev {}

             ;; uberjar task profile
             ;; The :default profile will be removed when generating uberjar
             :uberjar {:aot :all}}
  )