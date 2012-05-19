(ns netwars.aw-game
  (:require [netwars.game-board :as board]
            [netwars.aw-unit :as unit]
            [netwars.damagecalculator :as damage]
            [netwars.aw-map :as aw-map]
            [netwars.path :as path]
            [netwars.aw-player :as player]))

;; AwGame is a running game.
;; It stores info about the current players, whose turn it is, the unit spec used, etc.
;;
;; It works on a high level. Every move is logged, the game is stateful (using refs).
;; To move an unit, a path is needed. The functions in this namespace will automatically cause the necessary side effects (for example: consuming fuel).

(defrecord AwGame [info
                   current-player-index
                   round-counter
                   players
                   unit-spec
                   damagetable
                   board
                   current-unit         ;Stores the selected unit
                   moves                ;Every move in the game gets saved here
                   ])

;;; Game events

(defn game-events [game]
  (:moves game))

(defn log-event [game move]
  {:pre [(contains? move :type)]}
  (update-in game [:moves] conj move))

;;; Player Functions

(defn player-count [game]
  (count (:players game)))

(defn current-player [game]
  (get (:players game) (:current-player-index game)))

(defn next-player [game]
  (let [newgame (-> game
                    (update-in [:current-player-index]
                               (fn [idx]
                                 (if (>= (inc idx) (player-count game))
                                   0
                                   (inc idx))))
                    (update-in [:board] (fn [board]
                                          (reduce #(board/update-unit %1 %2 dissoc :moved)
                                                  board (keys (:units board)))))
                    (log-event {:type :turn-completed
                                :player (current-player game)}))]
    (if (zero? (:current-player-index newgame))
      (update-in newgame [:round-counter] inc)
      newgame)))

(defn get-player [game color]
  (first (filter #(= (:color %) color) (:players game))))

(defn update-player [game player-color f & args]
  (update-in game [:players]
             #(let [p (get-player game player-color)]
                (replace {p (apply f p args)} %))))

(defn remove-player
  "Removes a player in a running game.
 Removes all his units and makes his building neutral"
  [game player-color]

  (assert (not= player-color (:color (current-player game)))
          (str "Can't remove current-player with color " (name player-color)))

  (-> game
      ;; Remove terrain
      (update-in [:board] #(board/neutralize-buildings % player-color))

      ;; Remove units
      (update-in [:board] #(board/remove-units % player-color))

      ;; Remove the player
      (update-in [:players] (fn [seq] (remove #(= (:color %) player-color) seq)))))

(defn current-round
  "Returns the current round"
  [game]
  (:round-counter game))

;;; Utility function to run when a unit is moved/removed

(defn- check-capture-after-move [game c]
  (let [board (:board game)]
    (if (and (nil? (board/get-unit board c))
             (aw-map/is-building? (board/get-terrain board c)))
      (update-in game [:board] board/reset-capture c)
      game)))

;;; Fuel Costs

(defn fuel-costs
  "Returns the fuel costs for `path`"
  [game path]
  (let [board (:board game)
        unit (board/get-unit board (first path))]
    (assert unit (str "No unit on " (first path)))
    (when (path/valid-path? path board)
      (reduce + (map #(aw-map/movement-costs (board/get-terrain board %)
                                             (:movement-type (meta unit)))
                     (rest path))))))

;;; Movement Range and selected unit

(defn selected-coordinate
  "Returns the selected coordinate"
  [game]
  (:current-unit game))

(defn selected-unit
  "Returns the selected unit"
  [game]
  (board/get-unit (:board game) (:current-unit game)))

(defn select-unit
  "Sets the selected unit to unit at coordinate c."
  [game unit-coordinate]
  {:pre  [(-> game :board (board/get-unit unit-coordinate))]
   :post [(= unit-coordinate (:current-unit %))]}
  (assert (= (:color (current-player game))
             (-> game :board (board/get-unit unit-coordinate) :color))
          "Players can only select their own units")
  (assoc game :current-unit unit-coordinate))

(defn deselect-unit
  "Sets the currently selected unit of game to nil."
  [game]
  {:pre [(:current-unit game)]
   :post [(nil? (:current-unit %))]}
  (assoc game :current-unit nil))

(def movement-range-cache (atom {}))

(defn movement-range
  "Returns a set of all reachable fields for the currently selected unit."
  [game]
  (if (> (count @movement-range-cache) 50)
    (reset! movement-range-cache nil))

  (if-let [cached (get @movement-range-cache [(selected-coordinate game)
                                             (selected-unit game)])]
    cached
    (let [r (board/reachable-fields (:board game) (selected-coordinate game))]
      (swap! movement-range-cache assoc [(selected-coordinate game)
                                         (selected-unit game)] r)
      r)))

(defn wait-unit
  "Makes the unit 'miss' the turn. Actually assoc :moved to the
  currently selected unit and then deselects it."
  [game]
  {:pre [(selected-unit game)
         (not (:moved (selected-unit game)))]}
  (-> (update-in game [:board] board/update-unit (selected-coordinate game) assoc :moved true)
      (deselect-unit)))

(defn move-unit
  "Moves the currently selected unit along `path`.
`(last path)` must be in the current movement-range. Must be called in a transaction.
Returns path."
  [game path]
  {:pre [(path/path? path)
         (not (:moved (selected-unit game)))]}
  (assert (selected-unit game) "Tried to move unit without selected-unit")
  (assert (every? #(contains? (movement-range game) %) path)
          "Tried to move unit to non-reachable field")
  (assert (path/valid-path? path (:board game))
          "Given path isn't valid")

  (let [from (selected-coordinate game)
        to   (last path)
        fuel-costs (fuel-costs game path)]
   (-> game
       (update-in [:board] board/update-unit from
                  update-in [:fuel] - fuel-costs)
       ;; TODO: Deselection is wrong here; just update `current-unit'
       (assoc :current-unit to)
       (update-in [:board] board/move-unit from to) ;Important: First use fuel, then move
       (check-capture-after-move from)
       (log-event {:type :unit-moved
                   :from from
                   :to to
                   :fuel-costs fuel-costs}))))

(defn buy-unit [game c id-or-internal-name]
  (let [player (current-player game)
        unit (unit/make-unit (:unit-spec game) id-or-internal-name (:color player))
        price (:price (meta unit))]
    (assert (meta unit))
    (assert (nil? (board/get-unit (:board game) c))
            (str "Can't buy unit " (name (:internal-name unit)) "."
                 "There's already a unit on " c))
    (assert (<= price (:funds player))
            (str "Not enough funds to buy " (name (:internal-name unit))))
    (-> game
        (update-in [:board] board/add-unit c (assoc unit :moved true))
        (update-player (:color player) player/spend-funds price)
        (log-event {:type :bought-unit
                    :unit unit
                    :price price
                    :coordinate c}))))

;;; Capturing buildings

;;; TODO: Capture only works for selected-unit
(defn capture-building [game c]
  {:pre [(board/capture-possible? (:board game) c)
         (not (:moved (board/get-unit (:board game) c)))]}
  (-> game
      (update-in [:board] board/capture-building c)
      (update-in [:board] board/update-unit c assoc :moved true)))

;;; Attacking

(defn attack-possible? [game att-coord vic-coord]
  (let [board (:board game)
        att (board/get-unit board att-coord)
        def (board/get-unit board vic-coord)]
    (and att
         def
         (board/in-attack-range? board att-coord vic-coord)
         (not= (:color att) (:color def))
         (not (nil? (damage/choose-weapon (:damagetable game) att def))))))

(defn attackable-targets [game]
  (let [att-coord (selected-coordinate game)]
    (assert att-coord)
    (set (filter #(attack-possible? game att-coord %)
                 (board/attack-range (:board game) att-coord)))))

(defn perform-attack [game att-coord vic-coord & {:keys [counterattack]}]
  {:pre [(attack-possible? game att-coord vic-coord)
         (not (:moved (board/get-unit (:board game) att-coord)))]}
  (let [board (:board game)
        att (board/get-unit board att-coord)
        vic (board/get-unit board vic-coord)
        att-terr (board/get-terrain board att-coord)
        vic-terr (board/get-terrain board vic-coord)]
    (let [dam (damage/calculate-damage (:damagetable game)
                                       [att att-terr]
                                       [vic vic-terr])
          newvic (unit/apply-damage vic dam)
          board (:board game)
          newboard (-> (if newvic
                         (board/update-unit board vic-coord #(unit/apply-damage % dam))
                         (board/remove-unit board vic-coord))
                       (board/update-unit att-coord
                                          #(let [newu (unit/fire-weapon %
                                                                        (ffirst
                                                                         (damage/choose-weapon
                                                                          (:damagetable game)
                                                                          att
                                                                          vic)))]
                                             (if-not counterattack
                                               (assoc newu :moved true)
                                               newu))))]
      (let [newgame (-> game
                        (assoc :board newboard)
                        (check-capture-after-move vic-coord)
                        ;; TODO: Handle special case when vic is destroyed
                        (log-event {:type (if counterattack :counter-attack :attack)
                                    :from att-coord, :to vic-coord
                                    :attacker att, :victim newvic
                                    :damage dam}))]
        (if (and newvic
                 (board/in-attack-range? (:board newgame) vic-coord att-coord)
                 (not counterattack))
          (perform-attack newgame vic-coord att-coord :counterattack true)
          newgame)))))
