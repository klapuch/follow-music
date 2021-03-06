(defproject follow-music "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-http "3.10.0"]
                 [cheshire "5.9.0"]
                 [gorillalabs/neo4j-clj "4.1.0"]
                 [ring "1.8.2"]
                 [compojure "1.6.2"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot follow-music.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
