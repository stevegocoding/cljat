{:user
 {:plugins [[cider/cider-nrepl "0.11.0-SNAPSHOT"]
                  [refactor-nrepl "2.0.0-SNAPSHOT"]]
       
  :dependencies [[org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [spyscope "0.1.5"]
                 [leiningen #=(leiningen.core.main/leiningen-version)]
                 [alembic "0.3.2"]
                 [im.chit/vinyasa "0.4.2"]
                 [io.aviso/pretty "0.1.24"]]

  :injections [(require 'spyscope.core)
               (require '[vinyasa.inject :as inject])
               (inject/in ;; default injected namespace is '.'
                [vinyasa.inject :refer [inject [in inject-in]]]
                [vinyasa.lein :exclude [*project*]]

                ;; imports all functions in vinyasa.pull
                [alembic.still [distill pull]]

                ;; inject into clojure.core
                clojure.core
                [vinyasa.reflection .> .? .* .% .%> .& .>ns .>var]

                ;; inject into clojure.core with prefix
                clojure.core >
                [clojure.pprint pprint]
                [clojure.java.shell sh])]
  
  :repl-options {:nrepl-middleware [cider.nrepl.middleware.pprint/wrap-pprint
                                    cider.nrepl.middleware.pprint/wrap-pprint-fn]
                 :host "0.0.0.0"}
  }}
