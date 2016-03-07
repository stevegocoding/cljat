(defproject webapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ["-Xmx2g" "-XX:MaxPermSize=1G"  "-server"]
  
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.7.228"]
                 ;; [org.clojure/tools.namespace "0.2.11"]

                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]

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
                 [compojure "1.4.0"]

                 ;; component framework
                 [com.stuartsierra/component "0.3.1"]

                 ;; schema
                 [prismatic/schema "1.0.5"]]
  
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

  ;; Activated by uberjar task
  :uberjar {:aot :all}
  
  ;; Production Profile
  :prod {:open-browser? false
         :stacktracers? false
         :auto-reload? false}

  :profiles {
             :default [:base :system :user :provided :dev :dev-env]

             ;; Activated by default
             :dev {
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/java.classpath "0.2.3"]
                                  [ring/ring-devel "1.4.0"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-4"]]
                   }
             }
  )
