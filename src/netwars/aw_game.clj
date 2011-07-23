(ns netwars.aw-game
  (:use [netwars.game-board :as board]
        [netwars.aw-unit :as unit])
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
        board (board/generate-game-board loaded-map unit-spec)
        newplayers (clojure.core/map ref players)
        newinfo (assoc info :map mapsource)
        initial-event {:type :game-started
                       :info newinfo
                       :loaded-map loaded-map
                       :unit-spec unit-spec
                       :players players}]
    (AwGame. newinfo
             (ref 0)
             newplayers
             unit-spec
             (ref board)
             (ref [initial-event]))))

;;; Game events

(defn game-events [game]
  @(:moves game))

(defn log-event! [game move]
  {:pre [(contains? move :type)]}
  (alter (:moves game) conj move))

;;; Player Functions

(defn current-player [game]
  (nth (:players game) @(:current-player-index game)))

(defn next-player! [game]
  (log-event! game {:type :turn-completed
                    :player @(current-player game)})
  (alter (:current-player-index game)
         (fn [idx]
           (if (>= (inc idx) (count (:players game)))
             0
             (inc idx))))
  (current-player game))

;;; Attacking

