(ns netwars.aw-game
  (:use [netwars.game-board :as board]
        [netwars.aw-unit :as unit]
        [netwars.damagecalculator :as damage]
        [netwars.damagetable :as damagetable])
  (:require netwars.map-loader
            netwars.unit-loader))

(defrecord AwGame [info
                   current-player-index
                   players
                   unit-spec
                   damagetable
                   board
                   moves                ;Every move in the game gets saved here
                   ])

(defn make-game [info mapsource players]
  (let [loaded-map (netwars.map-loader/load-map mapsource)
        unit-spec (netwars.unit-loader/load-units "resources/units.xml")
        damagetable (damagetable/load-damagetable "resources/damagetable.xml")
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
             damagetable
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

(defn perform-attack! [game att-coord vic-coord & {:keys [counterattack]}]
  {:pre [(board/in-attack-range? @(:board game) att-coord vic-coord)]}
  (let [board @(:board game)
        att (board/get-unit board att-coord)
        vic (board/get-unit board vic-coord)
        att-terr (board/get-terrain board att-coord)
        vic-terr (board/get-terrain board vic-coord)]
    (let [dam (damage/calculate-damage (:damagetable game)
                                       [att att-terr]
                                       [vic vic-terr])
          newvic (unit/apply-damage vic dam)]
      (if newvic
       (alter (:board game) board/update-unit vic-coord #(unit/apply-damage % dam))
       (alter (:board game) board/remove-unit vic-coord))
      (when (and newvic
                 (board/in-attack-range? @(:board game) vic-coord att-coord)
                 (not counterattack))
        (perform-attack! game vic-coord att-coord :counterattack true))
      ;; TODO: Logging of events
      game)))
