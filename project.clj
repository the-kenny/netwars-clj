(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.1"]
                 [aleph "0.2.1-beta2"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [org.clojure/tools.logging "0.2.3"]
                 [log4j/log4j "1.2.16"]
                 [clj-logging-config "1.9.6" :exclusions [swank-clojure]]
                 [compojure "1.0.1"]
                 [hiccup "1.0.0"]
                 [ring/ring-devel "1.1.0"]
                 [swank-clojure "1.4.2"]]
  :exclusions [org.clojure/clojure]
  :extra-files-to-clean ["resoures/public/netwars.js"]
  :main netwars.core
  :cljsbuild {:crossovers [netwars.aw-game
                           netwars.aw-map
                           netwars.aw-player
                           netwars.aw-unit
                           netwars.damagecalculator
                           netwars.game-board
                           netwars.map-utils
                           netwars.map-renderer
                           netwars.path
                           netwars.pathfinding.a-star
                           netwars.tiles]
              :builds [{:source-path "src-cljs/"
                        :compiler
                        {:output-to "resources/public/netwars.js"
                         :foreign-libs [{:file "resources/public/dijkstra.js"
                                         :provides ["dijkstra"]}]
                         :pretty-print true
                         :optimizations :whitespace}}]}
  :profiles {:dev {:dependencies [[midje "1.3.1"]]
                   :plugins [[org.clojars.the-kenny/lein-midje "1.0.9"]]}}
  :plugins [[lein-cljsbuild "0.1.10"]])
