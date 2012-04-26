(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.1"]
                 [aleph "0.2.1-beta2"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [org.clojure/tools.logging "0.2.0"]
                 [log4j/log4j "1.2.16"]
                 [clj-logging-config "1.9.6"]
                 [compojure "1.0.1"]
                 [ring/ring-devel "1.0.1"]]
  :exclusions [org.clojure/clojure
               swank-clojure/swank-clojure]
  :extra-files-to-clean ["resoures/public/netwars.js"]
  :source-paths ["src/clj/"]
  ;; :main netwars.core
  :cljsbuild {:crossovers [netwars.aw-game
                           netwars.aw-map
                           netwars.aw-player
                           netwars.aw-unit
                           netwars.damagecalculator
                           netwars.game-board
                           netwars.map-utils
                           netwars.map-renderer
                           netwars.map-renderer-macro-hack
                           netwars.path
                           netwars.tiles]
              :builds [{:source-path "src/cljs/"
                        :compiler
                        {:output-to "resources/public/netwars.js"
                         :pretty-print true
                         :optimizations :whitespace}}]}
  :profiles {:dev {:dependencies [[midje "1.3.1"]]
                   :plugins [[org.clojars.the-kenny/lein-midje "1.0.9"]]}}
  :plugins [[lein-cljsbuild "0.1.8"]])
