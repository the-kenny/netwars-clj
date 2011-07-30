(ns netwars.game
  (:require [netwars.connection :as connection]))

(defmethod connection/handle-response :game-data [server message]
  (connection/log "got game data: " (str message)))

(defn request-game-data [server game-id]
  (connection/send-data server {:type :game-data
                                :game-id game-id}))

(defmethod connection/handle-response :new-game [server message]
  (connection/log "New game created!")
  (request-game-data server (:game-id message)))

(defn start-new-game [server map-name]
  (connection/send-data server {:type :new-game,
                                :map-name map-name}))
