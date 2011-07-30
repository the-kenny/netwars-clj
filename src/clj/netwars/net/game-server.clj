(ns netwars.net.game-server
  (:use [netwars.net.connection :as connection]
        [netwars.net.map-server :as map-server]
        [netwars.aw-game :as game]
        [netwars.player :as player]))

(defrecord ServerGame [aw-game clients])

(defn make-server-game [aw-game clients]
  (ServerGame. aw-game (ref clients)))

(defn assign-client! [aw-game player-index client]
  {:pre [(< player-index (count (:players aw-game)))]}
  (let [player-ref (nth (:players aw-game) player-index)]
    (alter player-ref with-meta {:connection-id (:client-id client)})))

(def running-games (atom {}))

(defn start-new-game [config first-client]
  (let [aw-game (game/make-game {} "maps/7330.aws" [(player/make-player "foo" :red 1000)
                                               (player/make-player "bar" :blue 1000)])
        id (java.util.UUID/randomUUID)]
    (assign-client! aw-game 0 first-client)
    (swap! running-games assoc id (make-server-game aw-game [first-client]))))

(defn send-units [client game]
  (let [units (-> game :aw-game :board deref :units)
        encoded (for [[k v] (seq units)] [[(:x k) (:y k)] (into {} v)])]
   (connection/send-data client {:type "unit-data"
                                 :units encoded})))

(defmethod connection/handle-request "new-game" [client request]
  (dosync
   (start-new-game nil client)))

(defmethod connection/handle-request "game-data" [client request]
  (println "got game-data request: " request)
  (when-let [game (get @running-games (java.util.UUID/fromString (:game-id request)))]
   (send-units client game)
   (map-server/send-map-data client "7330.aws")))
