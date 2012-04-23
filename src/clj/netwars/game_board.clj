(ns netwars.game-board
  (:require [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as unit]
            [clojure.set :as set]
            [netwars.damagecalculator :as damage]))

;; This namespace contains definitions and functions for GameBoard.
;; GameBoard combines AwMap with information about the units on it.
;;
;; It offers an unified interface for moving units in respect to the terrain.


;;; Game Board
(defrecord GameBoard [terrain units])

(defn map->GameBoard [m]
  (let [board (GameBoard. (aw-map/map->TerrainBoard (:terrain m))
                          (into {} (for [[c u] (:units m)] [c (with-meta (unit/map->AwUnit u)
                                                                (meta u))])))]
    (into board
          (apply dissoc m (keys board)))))

(defn make-game-board [terrain units]
  (GameBoard. terrain units))

(defn generate-game-board [game-map unit-spec]
  (let [terrain (:terrain game-map)
        units (zipmap (keys (:units game-map))
                      (map #(unit/make-unit unit-spec (:id %) (:color %))
                           (vals (:units game-map))))]
    (make-game-board terrain units)))

(defn get-terrain [^GameBoard board coord]
  (aw-map/at (:terrain board) coord))

(defn get-unit [^GameBoard board coord]
  (get (:units board) coord))

(defn add-unit [^GameBoard board coord unit]
  {:pre [(nil? (get-unit board coord))]
   :post [(= unit (get-unit % coord))]}
  (assoc-in board [:units coord] unit))

(defn update-unit [^GameBoard board coord f & args]
  {:pre [(not (nil? (get-unit board coord)))]}
  (assoc-in board [:units coord] (apply f (get-in board [:units coord]) args)))

(defn remove-unit [^GameBoard board coord]
  {:pre [(not (nil? (get-unit board coord)))]
   :post [(nil? (get-unit % coord))]}
  (assoc board :units (dissoc (:units board) coord)))

;;; TODO: Remove capture points
(defn remove-units [board color]
  (let [units (filter (fn [[c u]]
                            (= (:color u) color))
                          (:units board))]
    (reduce remove-unit board (map first units))))

(defn can-walk-on-field?
  "Returns true if unit can pass a field.
   Checks if the movement-type can pass the field and if an unit is on the field,
   it checks if both units have the same color."
  [board unit c]
  (assert (meta unit))
  (and (or (nil? (get-unit board c)) (= (:color unit) (:color (get-unit board c))))
       (aw-map/can-pass? (get-terrain board c) (:movement-type (meta unit)))))

(defn move-unit [^GameBoard board c1 c2]
  {:pre [(not (nil? (get-unit board c1))) (nil? (get-unit board c2))]
   :post [(nil? (get-unit % c1)) (not (nil? (get-unit % c2)))]}
  (let [u (get-unit board c1)]
   (assoc board :units (-> (:units board) (assoc c2 u) (dissoc c1)))))

(defn change-building-color [^GameBoard board c color]
  {:pre [(aw-map/is-building? (get-terrain board c))]
   :post [(= color (second (get-terrain % c)))]}
  (let [terrain (:terrain board)]
   (assoc board :terrain (aw-map/update-board terrain c
                                                (assoc (get-terrain board c) 1 color)))))

(defn neutralize-buildings [board color]
  (let [buildings (filter (fn [[c [typ col]]]
                            (= color col))
                          (aw-map/buildings (:terrain board)))]
    (reduce #(change-building-color %1 %2 :white) board (map first buildings))))

(defn capture-possible? [board c]
  (let [unit (get-unit board c)
        terrain (get-terrain board c)]
    (and (aw-map/is-building? terrain)
         (unit/can-capture? unit)
         (not= (:color unit)
               (second terrain)))))

(defn capture-building [board c]
  {:pre [(capture-possible? board c)]}
  (let [terrain (:terrain board)]
   (assoc board :terrain
          (aw-map/update-board terrain c
                               (aw-map/capture-building (get-terrain board c)
                                                        (:hp (get-unit board c))
                                                        (:color (get-unit board c)))))))

(defn reset-capture [board c]
  {:pre [(aw-map/is-building? (get-terrain board c))]}
  (let [terrain (:terrain board)]
   (assoc board :terrain
          (aw-map/update-board terrain c
                               (aw-map/reset-capture-points (get-terrain board c))))))

;;; Attacking

(defn attack-range [board att-coord]
  (let [att (get-unit board att-coord)
        area (apply set/union
                    (map :range (vals (unit/available-weapons att))))
        max-range (reduce max area)
        r (range (- max-range) (inc max-range))
        possibilities (for [x r, y r]
                        (aw-map/coord (+ (:x att-coord) x)
                                      (+ (:y att-coord) y)))]
    (-> (filter
         #(and (contains? area (aw-map/distance att-coord %))
               (aw-map/in-bounds? (:terrain board) %))
         possibilities)
        set
        (disj att-coord))))

(defn in-attack-range? [board att-coord vic-coord]
  (let [dist (aw-map/distance att-coord vic-coord)
        att (get-unit board att-coord)
        def (get-unit board vic-coord)]
    (contains? (apply set/union (map :range (vals (unit/available-weapons att))))
               (aw-map/distance att-coord vic-coord))))

;;; Movement Range

(defn reachable-fields
  "Returns a set of all coordinates reachable by unit on coordinate c"
  [board c]
  (assert (get-unit board c))
  (assert (meta (get-unit board c)))
  (let [unit (get-unit board c)
        movement-range (:movement-range (meta unit))
        movement-type (:movement-type (meta unit))
        fuel (:fuel unit)
        terrain (:terrain board)
        acc (atom #{})]
    (let [helper (fn helper [c rest initial?]
                   (let [t (get-terrain board c)
                         costs (if initial? 0 (aw-map/movement-costs t movement-type))]
                     (when (and
                            (aw-map/can-pass? t movement-type)
                            (or initial? (can-walk-on-field? board unit c))
                            (>= rest costs))
                       (let [newacc (swap! acc conj c)]
                        (when (> rest costs)
                          (let [{:keys [x y]} c
                                right (aw-map/coord (inc x)     y)
                                left  (aw-map/coord (dec x)     y)
                                down  (aw-map/coord      x (inc y))
                                up    (aw-map/coord      x (dec y))
                                restcosts (- rest costs)]
                            (when (aw-map/in-bounds? terrain right)
                              (helper right restcosts false))
                            (when (aw-map/in-bounds? terrain left)
                              (helper left  restcosts false))
                            (when (aw-map/in-bounds? terrain down)
                              (helper down  restcosts false))
                            (when (aw-map/in-bounds? terrain up)
                             (helper up    restcosts false))))))))]
      (helper c (min movement-range fuel) true))
    @acc))
