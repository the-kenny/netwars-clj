(ns netwars.net.game-server
  (:use [netwars.net.connection :as connection]
        [netwars.net.map-server :as map-server]
        [netwars.net.tiling :as tiling]
        [netwars.aw-game :as game]
        [netwars.aw-map :as aw-map]
        [netwars.game-board :as board]
        [netwars.player :as player]
        [netwars.path :as path]
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


;;; Helper macros

(def ^{:dynamic true} *unit* nil)
(def ^{:dynamic true} *game* nil)
(def ^{:dynamic true} *coordinate* nil)
(defmacro def-game-request [type [client-sym request-sym] & body]
  `(defmethod connection/handle-request ~type [client# request#]
     (if-let [game# (game-for-client client#)]
       (let [~client-sym client#,
             ~request-sym request#
             coord# (:coordinate request#)
             unit# (and coord# (board/get-unit @(:board game#) coord#))]
         (binding [*game* game#
                   *coordinate* coord#
                   *unit* unit#]
           (try ~@body
                (catch java.lang.Exception e#
                  (error e#))
                (catch java.lang.Error e#
                  (error e#)))))
       (error "Got" ~type "request while client" (:client-id client#)
              "isn't in a game"))))

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

(defn make-game-list-response []
  {:type :game-list
   :games (into {} (for [game (game-list)]
                     [(:game-id game) (:info game)]))})


(def-game-request :unit-info [client request]
  (when *unit*
    (info "Got request for unit info on coordinate: " *coordinate*)
    (connection/send-data client (assoc request :unit *unit*))))

;;; :helo Handler

(defmethod connection/handle-request :helo [client request]
  (info "We got a new client:" (:client-id client))
  ;; Send game-list
  (send-data client (make-game-list-response)))

;;; Creating/Joining of games

(connection/defresponse :game-list [client _]
  ;; TODO: Filter out games where the client doesn't have access
  (info "Sending game-list to client" (:client-id client))
  (make-game-list-response))

(defmethod connection/handle-request :new-game [client request]
  (info "Got new-game request:" request "from client:" (:client-id client))
  (let [id (java.util.UUID/randomUUID)
        aw-game (-> (start-new-game request)
                    (assoc :game-id id)
                    (dissoc :type))]
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

(def-game-request :select-unit [client request]
  (when *unit*
    (info ":select-unit " (:game-id *game*) " " *coordinate*)
    (dosync (game/select-unit! *game* *coordinate*))
    (connection/send-data client (assoc request :coordinate *coordinate*))
    (connection/send-broadcast (broadcast-for-game *game*)
                               {:type :movement-range
                                :movement-range (game/movement-range *game*)})))

(def-game-request :deselect-unit [client request]
  (if (game/selected-unit *game*)
    (do
     (info ":deselect-unit " (:game-id *game*))
     (dosync (game/deselect-unit! *game*))
     (connection/send-broadcast (broadcast-for-game *game*) request))
    (error "Got :deselect-unit without a selected unit on coordinate " *coordinate* "in game" (:game-id *game*))))

(def-game-request :movement-range [client request]
  (let [board (-> *game* :board deref)]
    (if *unit*
      (let [fields (board/reachable-fields board *coordinate*)]
        (info "Client" (:client-id client) "clicked on unit" *unit* "at" *coordinate*)
        (connection/send-broadcast (broadcast-for-game *game*)
                                   (assoc request :movement-range fields)))
      (error "Can't request movement-range for coordinate: No unit on" *coordinate*))))


(def-game-request :move-unit [client request]
  (let [from (game/selected-coordinate *game*)
        path (path/make-path (:path request))
        board @(:board *game*)]
    (assert (= from (first path)))
    (assert (board/get-unit board from))
    (assert (nil? (board/get-unit board (last path))))
    (if (path/valid-path? path board)
      (let [fuel-costs (game/fuel-costs *game* path)]
       (info "Moving unit along" path)
       (dosync (game/move-unit! *game* path))
       (connection/send-broadcast (broadcast-for-game *game*)
                                  (assoc request
                                    :valid true
                                    :from from
                                    :to (last path)
                                    :path (:path request)
                                    :fuel-costs fuel-costs)))
      (do
        (error "Invalid path: " path "in" (:game-id *game*))
        (connection/send-broadcast (broadcast-for-game *game*)
                                   (assoc request :valid false))))))
