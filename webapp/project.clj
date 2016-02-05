(defproject webapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.7.228"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.4.0"]
                 [mount "0.1.9"]]
  
  :plugins [[cider/cider-nrepl "0.11.0-SNAPSHOT"]
            [refactor-nrepl "2.0.0-SNAPSHOT"]
            [lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-6"]]

  :source-paths ["src", "src-cljs"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/js/app.js"
                                   :optimizations :none
                                   :pretty-print true}}]}

  :figwheel {:http-server-root "public"
             :port 3449
             :css-dirs ["resources/public/css"]}

  :ring {:handler webapp.core/app-routes
         :uberwar-name "cljat.war"}
  
  :profiles {;; Development Profile
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.nrepl "0.2.11"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/java.classpath "0.2.3"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-4"]]
                   
                   :repl-options {:host "0.0.0.0"
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :ring {:nrepl {:start? true
                                  :host "0.0.0.0"
                                  :port 7888}}}
             ;; Test Profile
             :test {}

             ;; Production Profile
             :production {:open-browser? false
                          :stacktracers? false
                          :auto-reload? false}}
  )
