(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.0"]
                 [aleph "0.3.0-beta7"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [org.clojure/tools.logging "0.2.3"]
                 [log4j/log4j "1.2.16"]
                 [clj-logging-config "1.9.6" :exclusions [swank-clojure]]
                 [compojure "1.0.1"]
                 [hiccup "1.0.0"]
                 [ring/ring-devel "1.1.0"]
                 [ring-edn "0.1.0"]

                 [org.clojure/tools.nrepl "0.2.0-RC1"]
                 [clojure-complete "0.2.2"]
                 
                 ;; Clojurescript stuff
                 [org.clojure/clojurescript "0.0-1535"]
                 [org.clojure/google-closure-library "0.0-2029"] 
                 [org.clojure/google-closure-library-third-party "0.0-2029"]] 
  :exclusions [org.clojure/clojure]
  :extra-files-to-clean ["resoures/public/netwars.js"]
  :main netwars.core
  :repl-options {:port 7888}
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
                         :pretty-print true
                         :warnings true
                         :optimizations :whitespace}}]}
  :profiles {:dev {:dependencies [[midje "1.3.1"]]
                   :plugins [[lein-midje "2.0.0-SNAPSHOT"]]}}
  :plugins [[lein-cljsbuild "0.2.9"]])
