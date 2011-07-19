(ns netwars.aw-game
  (:use netwars.game-board)
  (:require netwars.map-loader
            netwars.unit-loader))

(defrecord AwGame [info
                   current-player-index
                   players
                   unit-spec
                   board
                   moves                ;Every move in the game gets saved here
                   ])

(defn make-game [info mapsource players]
  (let [loaded-map (netwars.map-loader/load-map mapsource)
        unit-spec (netwars.unit-loader/load-units "resources/units.xml")
        board (generate-game-board loaded-map unit-spec)
        newplayers (clojure.core/map ref players)]
    (AwGame. (assoc info :map mapsource)
             (ref 0)
             newplayers
             unit-spec
             (ref board)
             (ref []))))

(defn current-player [game]
  (nth (:players game) @(:current-player-index game)))

(defn next-player! [game]
  (alter (:current-player-index game)
         (fn [idx]
           (if (>= (inc idx) (count (:players game)))
             0
             (inc idx))))
  (current-player game))
