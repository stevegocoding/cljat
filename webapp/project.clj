(defproject webapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [compojure "1.4.0"]]
  :plugins [[cider/cider-nrepl "0.11.0-SNAPSHOT"]
            [refactor-nrepl "2.0.0-SNAPSHOT"]
            [lein-ring "0.9.7"]]
  :ring {:handler webapp.core/app-routes
         :uberwar-name "cljat.war"}
  :profiles{:dev
            {:source-paths ["dev"]
             :dependencies [[org.clojure/tools.namespace "0.2.11"]
                            [org.clojure/java.classpath "0.2.3"]]}
            :production
            {:open-browser? false
             :stacktracers? false
             :auto-reload? false}})
