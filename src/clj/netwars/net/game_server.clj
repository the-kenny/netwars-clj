(ns netwars.net.game-server
  (:use [netwars.net.connection :as connection]
        [netwars.net.map-server :as map-server]
        [netwars.net.tiling :as tiling]
        [netwars.aw-game :as game]
        [netwars.player :as player]))

(def map-base-path "maps/")

(defrecord ServerGame [aw-game clients])

(defn make-server-game [aw-game clients]
  (ServerGame. aw-game (ref clients)))

(defn assign-client! [aw-game player-index client]
  {:pre [(< player-index (count (:players aw-game)))]}
  (let [player-ref (nth (:players aw-game) player-index)]
    (alter player-ref with-meta {:connection-id (:client-id client)})))

(def running-games (atom {}))

(defn game-list []
  (for [[id game] @running-games]
    [id (-> game :aw-game :info)]))

(defmethod connection/handle-request :game-list [client request]
  ;; TODO: Filter out games where the client doesn't have access
  (connection/send-data client (assoc request :games (game-list))))

(defn start-new-game [config first-client]
  (println "start-new-game:" config)
  (let [aw-game (game/make-game {} (str map-base-path (:map-name config))
                                [(player/make-player "foo" :red 1000)
                                 (player/make-player "bar" :blue 1000)])
        id (java.util.UUID/randomUUID)]
    (assign-client! aw-game 0 first-client)
    (swap! running-games assoc id (make-server-game aw-game [first-client]))
    id))

(defmethod connection/handle-request :new-game [client request]
  (let [id (dosync (start-new-game request client))]
    (connection/send-data client (assoc request :game-id (str id)))))

(defn send-units [client game]
  (let [units (-> game :aw-game :board deref :units)]
   (connection/send-data client {:type :unit-data
                                 :units units})))

(defmethod connection/handle-request :game-data [client request]
  (println "got game-data request: " request)
  (when-let [game (get @running-games (java.util.UUID/fromString (:game-id request)))]
    (connection/send-data client (assoc request :info (-> game :aw-game :info)))
    (map-server/send-map-data client (-> game :aw-game :board deref))
    (send-units client game)))
