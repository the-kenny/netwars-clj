(ns netwars.aw-game
  (:use [netwars.game-board :as board]
        [netwars.aw-unit :as unit]
        [netwars.damagecalculator :as damage]
        [netwars.damagetable :as damagetable]
        [netwars.aw-map :as aw-map]
        [netwars.path :as path])
  (:require netwars.map-loader
            netwars.unit-loader))

(defrecord AwGame [info
                   current-player-index
                   players
                   unit-spec
                   damagetable
                   board
                   current-unit         ;Stores the selected unit
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
             (ref nil)
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
      (log-event! game {:type (if counterattack :counter-attack :attack)
                        :from att-coord, :to vic-coord
                        :attacker att, :victim newvic
                        :damage dam})
      (when (and newvic
                 (board/in-attack-range? @(:board game) vic-coord att-coord)
                 (not counterattack))
        (perform-attack! game vic-coord att-coord :counterattack true))
      game)))

;;; Fuel Costs

(defn fuel-costs
  "Returns the fuel costs for `path`"
  [game path]
  (let [board @(:board game)
        unit (board/get-unit board (first path))]
    (when (nil? unit)
      (throw (java.lang.IllegalArgumentException. (str "No unit on " (first path)))))
    (when (valid-path? path board)
      (reduce + (map #(aw-map/movement-costs (board/get-terrain board %)
                                             (:movement-type (meta unit)))
                     (rest path))))))

;;; Movement Range and selected unit

(defn selected-coordinate
  "Returns the selected coordinate"
  [game]
  @(:current-unit game))

(defn selected-unit
  "Returns the selected unit"
  [game]
  (board/get-unit @(:board game) @(:current-unit game)))

;;; TODO: Make sure that only the current player can select units
(defn select-unit!
  "Sets the selected unit to unit at coordinate c. Must be called in a transaction."
  [game unit-coordinate]
  {:pre [(-> game :board deref (board/get-unit unit-coordinate))]
   :post [(= unit-coordinate @(:current-unit game))]}
  (ref-set (:current-unit game) unit-coordinate))

(defn deselect-unit!
  "Sets the currently selected unit of game to nil. Returns the last value."
  [game]
  (let [c @(:current-unit game)]
    (ref-set (:current-unit game) nil)
    c))

(defn movement-range
  "Returns a set of all reachable fields for the currently selected unit."
  [game]
  (board/reachable-fields @(:board game) (selected-coordinate game)))

(defn move-unit!
  "Moves the currently selected unit to `to`.
`to` must be in the current movement-range. Returns to."
  [game path]
  (when-not (selected-unit game)
    (throw (java.lang.IllegalStateException.
            "Tried to move unit without selected-unit")))
  (when-not (every? (partial contains? (movement-range game)) path)
    (throw (java.lang.IllegalStateException.
            "Tried to move unit to non-reachable field")))
  (when-not (valid-path? path @(:board game))
    (throw (java.lang.IllegalStateException.
            "Given path isn't valid")))
  (let [from (selected-coordinate game)
        to   (last path)]
   (alter (:board game) board/move-unit from to)
   (alter (:board game) board/update-unit to update-in [:fuel] - (fuel-costs game path))
   (log-event! game {:type :unit-moved
                     :from from
                     :to to}))
  path)
