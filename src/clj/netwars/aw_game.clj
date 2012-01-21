(ns netwars.aw-game
  (:use [netwars.game-board :as board]
        [netwars.aw-unit :as unit]
        [netwars.damagecalculator :as damage]
        [netwars.damagetable :as damagetable]
        [netwars.aw-map :as aw-map]
        [netwars.path :as path]
        [netwars.map-loader :only [load-map]]
        [netwars.unit-loader :only [load-units]]
        [netwars.aw-player :as player]))

;; AwGame is a running game.
;; It stores info about the current players, whose turn it is, the unit spec used, etc.
;;
;; It works on a high level. Every move is logged, the game is stateful (using refs).
;; To move an unit, a path is needed. The functions in this namespace will automatically cause the necessary side effects (for example: consuming fuel).

(defrecord AwGame [info
                   current-player-index
                   players
                   unit-spec
                   damagetable
                   board
                   current-unit         ;Stores the selected unit
                   moves                ;Every move in the game gets saved here
                   ])

(def +default-funds+ 1000)

(defn- sort-colors [colors]
  (filter (set colors) [:red :blue :yellow :green :black]))

(defn make-game [info mapsource]
  (let [loaded-map (load-map mapsource)
        unit-spec (load-units "resources/units.xml")
        damagetable (damagetable/load-damagetable "resources/damagetable.xml")
        board (board/generate-game-board loaded-map unit-spec)
        newinfo (assoc info :map mapsource)
        players (map #(player/make-player %1 %2 +default-funds+)
                     (map #(str "Player " %) (range 1 1000))
                     (sort-colors (-> loaded-map :info :player-colors)))
        initial-event {:type :game-started
                       :info newinfo
                       :loaded-map loaded-map
                       :unit-spec unit-spec
                       :players players}]
    (AwGame. newinfo
             (ref 0)
             (ref (vec players))
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
  (get @(:players game) @(:current-player-index game)))

(defn next-player! [game]
  (log-event! game {:type :turn-completed
                    :player (current-player game)})
  (alter (:current-player-index game)
         (fn [idx]
           (if (>= (inc idx) (count @(:players game)))
             0
             (inc idx))))
  (current-player game))

(defn get-player [game color]
  (first (filter #(= (:color %) color) @(:players game))))

(defn update-player! [game player-color f & args]
  (alter (:players game) #(let [p (get-player game player-color)]
                            (replace {p (apply f p args)} %)))
  (get-player game player-color))

(defn remove-player!
  "Removes a player in a running game.
 Removes all his units and makes his building neutral"
  [game player-color]

  (when (= player-color (:color (current-player game)))
    (throw (IllegalArgumentException.
            (str "Can't remove current-player with color " (name player-color)))))

  (dosync
   ;; Remove units
   ;; TODO: Use dounits
   (doseq [[c u] (:units @(:board game))]
     (when (= player-color (:color u))
      (alter (:board game) board/remove-unit c)))
   ;; Remove terrain
   ;; TODO: Use dobuildings
   (let [terrain-board (-> game :board deref :terrain)]
    (doseq [x (range (width terrain-board)), y (range (height terrain-board))
            :let [c (coord x y)
                  t (at terrain-board c)]]
      (when (and (is-building? t) (= player-color (second t)))
        (alter (:board game) board/change-building-color c :white ))))
   ;; Remove the player
   (let [player-to-remove (first (filter #(= player-color (:color %))
                                         @(:players game)))]
     (alter (:players game) (fn [seq] (remove #(= (:color %) player-color) seq)))
     player-to-remove)))

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
      ;; This is ugly.
      ;; We have to use a non-nice fun from damagecalculator to choose the weapon
      (let [[main-or-alt _] (first (damage/choose-weapon (:damagetable game) att vic))]
       (alter (:board game)
              board/update-unit
              att-coord
              #(unit/fire-weapon % main-or-alt)))
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
  "Moves the currently selected unit along `path`.
`(last path)` must be in the current movement-range. Must be called in a transaction.
Returns path."
  [game path]
  {:pre [(path/path? path)]}
  (when-not (selected-unit game)
    (throw (java.lang.IllegalStateException.
            "Tried to move unit without selected-unit")))
  (when-not (every? #(contains? (movement-range game) %) path)
    (throw (java.lang.IllegalArgumentException.
            "Tried to move unit to non-reachable field")))
  (when-not (path/valid-path? path @(:board game))
    (throw (java.lang.IllegalArgumentException.
            "Given path isn't valid")))
  (let [from (selected-coordinate game)
        to   (last path)
        fuel-costs (fuel-costs game path)]
   (alter (:board game) board/update-unit from
          update-in [:fuel] - fuel-costs)
   (alter (:board game) board/move-unit from to) ;Important: First use fuel, then move
   (log-event! game {:type :unit-moved
                     :from from
                     :to to
                     :fuel-costs fuel-costs}))
  path)

(defn buy-unit! [game c id-or-internal-name]
  (let [player (current-player game)
        unit (unit/make-unit (:unit-spec game) id-or-internal-name (:color player))
        price (:price (meta unit))]
    (cond
     (board/get-unit @(:board game) c)
     (throw (IllegalStateException.
             (str "Can't buy unit " (name (:internal-name unit)) ". There's already a unit on " c)))

     (> price (:funds player))
     (throw (IllegalStateException.
             (str "Not enough funds to buy " (name (:internal-name unit)))))

     (<= price (:funds player))
     (do (alter (:board game) board/add-unit c unit)
         (update-player! game (:color player) player/spend-funds price)
         unit))))
