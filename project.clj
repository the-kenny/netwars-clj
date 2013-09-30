(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]
                 [http-kit "2.1.10"]
                 [ring/ring-core "1.2.0"]
                 [fogus/ring-edn "0.2.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/clojurescript "0.0-1909"]]
  :extra-files-to-clean ["resoures/public/netwars.js"]
  :main netwars.core
  :repl-options {:port 7888}
  :cljsbuild
  {:builds
   [{:source-paths ["src-cljs/"],
     :compiler
     {:pretty-print true,
      :output-to "resources/public/netwars.js",
      :warnings true,
      :optimizations :whitespace}}],
   :crossovers [netwars.aw-game
                netwars.aw-map
                netwars.aw-player
                netwars.aw-unit
                netwars.damagecalculator
                netwars.game-board
                netwars.map-utils
                netwars.map-renderer
                netwars.path
                netwars.pathfinding.a-star
                netwars.tiles]}

  :profiles {:dev {:dependencies [[clojure-complete "0.2.3"]]}}
  :plugins [[lein-cljsbuild "0.3.3"]])
