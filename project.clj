(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [aleph "0.2.1-SNAPSHOT"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [org.clojure/tools.logging "0.2.0"]
                 [log4j/log4j "1.2.16"]
                 [clj-logging-config "1.9.6"
                  :exclusions [swank-clojure/swank-clojure]]
                 [compojure "1.0.0" :exclusions [org.clojure/clojure]]
                 [ring/ring-devel "1.0.1"]
                 [org.clojure/data.json "0.1.1"]]
  :dev-dependencies [[emezeske/lein-cljsbuild "0.0.1"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"}
  :source-path "src/clj/"
  :main netwars.core
  :cljsbuild {:source-dir "src/cljs"
              :output-file "resources/public/netwars.js"
              :optimizations :whitespace
              :pretty-print true})
