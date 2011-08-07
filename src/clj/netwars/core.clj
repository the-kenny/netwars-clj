(ns netwars.core
  (:use [netwars.net.connection :as connection]
        [netwars.net.game-server :as game-server]
        clojure.tools.logging
        clj-logging-config.log4j))

(set-logger!
 "netwars.net"
 :level :info
 :pattern "%d %p: %m%n")

(defn -main []
  (connection/start-server 8080))
