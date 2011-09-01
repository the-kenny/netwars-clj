(ns netwars.net.game-server
  (:use [netwars.net.connection :as connection]
        [netwars.net.map-server :as map-server]
        [netwars.net.tiling :as tiling]
        [netwars.aw-game :as game]
        [netwars.aw-map :as aw-map]
        [netwars.game-board :as board]
        [netwars.player :as player]
        [clojure.tools.logging :only [debug info warn error fatal]]))

;;; Overview:
;;; A client connects and gets a client-id.
;;; The client can request a list of tuples [game-id game-info] using the game-list req.

;;; Joining a game
;;; The client can join a game using the join-game request
;;; The server will send all actions in a game to all clients which joined it
;;; client-game-map will map client-ids to game-ids

;;; Broadcasting Game-Events to clients
;;; Every game has a key :broadcast-channel to which messages can be sent using
;;; connection/send-broadcast

(def map-base-path "maps/")

;;; Game List handling

(def running-games (ref {}))            ;Maps a game-id to a netwars.aw-game/AwGame
(def client-game-map (ref {}))          ;Maps a client-id to a game-id

(defn game-list
  "Returns a list of AwGames with :game-id attached to every game"
  []
  {:post [(every? :game-id %)]}
  (vals @running-games))

(defn store-game!
  "Stores an AwGame in the running-games sore. The game needs a unique id in :game-id"
  [game]
  {:pre [(:game-id game)]}
  (info "Stored game:" (:info game))
  (alter running-games assoc (:game-id game) game))

(defn get-game
  "Returns the AwGame with the game-id id"
  [id]
  (get @running-games id))

(defn broadcast-for-game [game]
  (get game :broadcast-channel))

(defn game-for-client
  "Returns the game a client currently spectates"
  [client]
  (let [client-id (:client-id client)]
    (get-game (get @client-game-map client-id))))

(defn dissoc-client!
  "Removed client from the game he currently spectates.
   No-op when client spectates no game."
  [client]
  (info "Removing client" (:client-id client) "from all games")
  (when-let [game (game-for-client client)]
    (info "Removing client" (:client-id client)
          "from game-broadcast for game" (:game-id game) )
    (connection/remove-broadcast-receiver! (broadcast-for-game game) client))
  (alter client-game-map dissoc (:client-id client)))

(defn assign-client!
  "Assigns client to game. The client will receive all events from the game."
  [client game]
  {:pre [client game (:game-id game)]}
  (dissoc-client! client)                ;Dissoc from previous game
  (info "Assigning client" (:client-id client) "to game" (:game-id game))
  (alter client-game-map assoc (:client-id client) (:game-id game))
  (connection/add-broadcast-receiver! (broadcast-for-game game) client))

(defn start-new-game
  "Creates an AwGame with parameters from its argument"
  [config]
  (let [game (game/make-game config (str map-base-path (:map-name config)) [])
        broadcast (connection/make-broadcast-channel)]
    (assoc game :broadcast-channel broadcast)))

;;; Connection-Handling

(defn disconnect-client
  "Calls dissoc-client! to remove client from the game he spectates"
  [client]
  (when-let [game (game-for-client client)]
   (dosync (dissoc-client! client))))
(connection/on-disconnect #'disconnect-client)

;;; Game-related Send Functions

(defn send-units [client game]
  (let [units (-> game :board deref :units)]
    (info "Sending units from game" (:game-id game) "to client" (:client-id client))
    (connection/send-data client {:type :unit-data
                                  :units units})))

(defn send-game-data [game client]
  (info "Sending game-data for game" (:game-id game) "to client" (:client-id client))
  (connection/send-data client {:type :game-data
                                :info (:info game)})
  (map-server/send-map-data client (-> game :board deref))
  (send-units client game))

;;; Creating/Joining of games

(connection/defresponse :game-list [client _]
  ;; TODO: Filter out games where the client doesn't have access
  (info "Sending game-list to client" (:client-id client))
  {:games (into {} (for [game (game-list)]
                     [(:game-id game) (:info game)]))})

(defmethod connection/handle-request :new-game [client request]
  (info "Got new-game request:" request "from client:" (:client-id client))
  (let [id (java.util.UUID/randomUUID)
        aw-game (assoc (start-new-game request) :game-id id)]
    (dosync
     (store-game! aw-game)
     (assign-client! client aw-game))
    (connection/send-data client (assoc request :game-id id))
    (send-game-data aw-game client)
    (connection/send-broadcast connection/broadcast-channel
                                {:type :new-listed-game
                                 :game (select-keys aw-game [:game-id :info])})))

(defmethod connection/handle-request :join-game [client request]
  (when-let [game (get-game (java.util.UUID/fromString (:game-id request)))]
    (info "Got join-game request from client" (:client-id client)
          "for game" (:game-id game))
    (dosync
     (assign-client! client game))
    (send-game-data game client)))

(defmethod connection/handle-request :game-data [client request]
  (info "got game-data request:" request)
  (when-let [game (get-game (java.util.UUID/fromString (:game-id request)))]
    (send-game-data game client)))

;;; Unit Requests

(defmethod connection/handle-request :unit-clicked [client request]
  (if-let [game (game-for-client client)]
   (let [c (apply aw-map/coord (:coordinate request))
         board (-> game :board deref)]
     (if-let [unit (get-unit board c)]
       (do
        (info "Client" (:client-id client) "clicked on unit at" c)
        (dosync
         (game/select-unit! game c))
        (connection/send-broadcast (broadcast-for-game game)
                                   {:type :movement-range
                                    :coordinate c
                                    :movement-range (game/movement-range game)}))
       (error ":unit-clicked from client" (:client-id client) "without unit on" c)))
   (warn "Request for movement-range while client"
         (:client-id client) "isn't in a game")))

(defmethod connection/handle-request :terrain-clicked [client request]
  (if-let [game (game-for-client client)]
    (let [c (apply aw-map/coord (:coordinate request))]
     (cond
      ;; (and (game/selected-unit game) (contains? (game/movement-range game) c))
      ;; (let [from (game/selected-coordinate game), to c]
      ;;   (game/move-unit! game to)
      ;;   (connection/send-broadcast (broadcast-for-game game)
      ;;                              {:type :move-unit
      ;;                               :from from
      ;;                               :to to}))

      (and (game/selected-unit game) (not (contains? (game/movement-range game) c)))
      (do (dosync (game/deselect-unit! game))
          (connection/send-broadcast (broadcast-for-game game)
                                     {:type :deselect-unit
                                      :coordinate c}))))
    (warn "Request for movement-range while client"
          (:client-id client) "isn't in a game")))

(comment (defmethod connection/handle-request :movement-range [client request]
   (if-let [game (game-for-client client)]
     (let [c (apply aw-map/coord (:coordinate request))
           board (-> game :board deref)]
       (if-let [unit (get-unit board c)]
         (let [fields (board/reachable-fields board c)]
           (info "Client" (:client-id client) "clicked on unit" unit "at" c)
           (connection/send-broadcast (broadcast-for-game game)
                                      (assoc request :movement-range fields)))
         (warn "Can't request movement-range for coordinate: No unit on" c)))
     (warn "Request for movement-range while client"
           (:client-id client) "isn't in a game"))))

(comment (defmethod connection/handle-request :move-unit [client request]
   (info "Got request to move unit from" (:from request) "to" (:to request))
   (if-let [game (game-for-client client)]
     (let [board (-> game :board deref)
           from (apply aw-map/coord (:from request))
           to   (apply aw-map/coord (:to   request))]

       (dosync
        ;; TODO: Error Handling
        (alter (:board game) board/move-unit from to))
       (connection/send-broadcast (broadcast-for-game game)
                                  (assoc request :valid true))))))
