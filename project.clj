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
  :dev-dependencies [[lein-cljsbuild "0.0.8"]
                     [midje "1.3.1"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"}
  :source-path "src/clj/"
  :main netwars.core
  :cljsbuild {:source-path "src/cljs/"
              :crossovers [netwars.aw-map
                           netwars.aw-unit
                           netwars.game-board]
              :compiler {:output-to "resources/public/netwars.js"
                         :foreign-libs [{:file "resources/public/kinetic.js"
                                         :provides ["kinetic"]}]
                         :optimizations :whitespace
                         :pretty-print true}})
