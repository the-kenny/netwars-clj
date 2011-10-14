(defproject netwars "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [aleph "0.2.0-beta3-SNAPSHOT"]
                 [commons-codec "1.5" :exclusions [commons-logging]]
                 [org.clojure/tools.logging "0.2.0"]
                 [log4j/log4j "1.2.16"]
                 [clj-logging-config "1.5"]
                 [compojure "0.6.5"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"}
  :source-path "src/clj/"
  :main netwars.core)
