(ns netwars.core
  (:use [netwars.net.connection :as connection]
        [netwars.net.game-server :as game-server]))

(defn -main [port]
  (connection/start-server (Integer/parseInt port)))
