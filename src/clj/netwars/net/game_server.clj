(ns netwars.net.game-server
  (:use [netwars.net.connection :as connection]
        [netwars.net.map-server :as map-server]
        [netwars.net.tiling :as tiling]
        [netwars.aw-game :as game]
        [netwars.player :as player]))

;;; Overview:
;;; A client connects and gets a client-id.
;;; The client can request a list of tuples [game-id game-info] using the game-list req.

;;; Joining a game
;;; The client can join a game using the join-game request
;;; The server will send all actions in a game to all clients which joined it
;;; client-game-map will map client-ids to game-ids

(def map-base-path "maps/")

;;; Game List handling

(def running-games (ref {}))            ;Maps a game-id to a netwars.aw-game/AwGame
(def client-game-map (ref {}))       ;Maps a client-id to a game-id

(defn game-list
  "Returns a list of AwGames with :game-id attached to every game"
  []
  {:post [(every? :game-id %)]}
  (vals @running-games))

(defn store-game!
  "Stores an AwGame in the running-games sore. The game needs a unique id in :game-id"
  [game]
  {:pre [(:game-id game)]}
  (alter running-games assoc (:game-id game) game))

(defn get-game
  "Returns the AwGame with the game-id id"
  [id]
  (get @running-games id))

(defn assign-client!
  "Assigns client to game. The client will receive all events from the game."
  [client game]
  {:pre [client game (:game-id game)]}
  (alter client-game-map assoc (:client-id client) (:game-id game)))

(defn dissoc-client!
  "Removed client from the game he currently spectates"
  [client]
  (alter client-game-map dissoc (:client-id client)))

(defn game-for-client
  "Returns the game a client currently spectates"
  [client]
  (let [client-id (:client-id client)]
    (get-game (get client-game-map client-id))))

(defn start-new-game
  "Creates an AwGame with parameters from its argument"
  [config]
  (game/make-game config (str map-base-path (:map-name config)) []))

;;; Connection-Handling

(defn disconnect-client
  "Calls dissoc-client! to remove client from the game he spectates"
  [client]
  (when-let [game (get-game (:client-id client))]
   (println "Removing client" (:client-id client) "from game" (:game-id game))
   (dosync (dissoc-client! client))))
(connection/on-disconnect #'disconnect-client)

;;; Client requests

(defn send-units [client game]
  (let [units (-> game :board deref :units)]
   (connection/send-data client {:type :unit-data
                                 :units units})))

(defn send-game-data [game client]
  (connection/send-data client {:type :game-data
                                :info (:info game)})
  (map-server/send-map-data client (-> game :board deref))
  (send-units client game))

(defmethod connection/handle-request :game-list [client request]
  ;; TODO: Filter out games where the client doesn't have access
  (let [info-map (into {} (for [game (game-list)] [(:game-id game) (:info game)]))]
   (connection/send-data client (assoc request :games info-map))))

(defmethod connection/handle-request :new-game [client request]
  (let [id (java.util.UUID/randomUUID)
        aw-game (assoc (start-new-game request) :game-id id)]
    (dosync
     (store-game! aw-game)
     (assign-client! client aw-game))
    (connection/send-data client (assoc request :game-id id))
    (send-game-data aw-game client)))

(defmethod connection/handle-request :join-game [client request]
  (when-let [game (get-game (java.util.UUID/fromString (:game-id request)))]
    (dosync
     (assign-client! client game))
    (println "Assigned client" (str (:client-id client)) "to game" (:game-id request))
    (send-game-data game client)))

(defmethod connection/handle-request :game-data [client request]
  (println "got game-data request: " request)
  (when-let [game (get-game (java.util.UUID/fromString (:game-id request)))]
    (send-game-data game client)))
