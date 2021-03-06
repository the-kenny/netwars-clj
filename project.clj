(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]
                 [http-kit "2.1.10"]
                 [ring/ring-core "1.2.0" :exclusions [org.clojure/tools.reader]]
                 [fogus/ring-edn "0.2.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/clojurescript "0.0-2075"]
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]
                 [prismatic/dommy "0.1.2"]]
  :extra-files-to-clean ["resoures/public/netwars.js"]
  :main netwars.core
  :repl-options {:port 7888}
  :cljsbuild
  {:builds
   [{:source-paths ["src-cljs/"]
     :compiler
     {:pretty-print false
      :warnings true,
      :optimizations :whitespace
      :output-to "resources/public/js/netwars.js"
      :output-dir "resources/public/js/"
      ;; :source-map "resources/public/js/main.js.map"
      ;; :source-map-path ""
      }}]
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
  :plugins [[lein-cljsbuild "1.0.0" :exclusions [org.clojure/clojurescript]]])
