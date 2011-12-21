(ns netwars.game-board
  (:use netwars.aw-map
        netwars.aw-unit))

;;; Game Board
(defrecord GameBoard [terrain units])

(defn make-game-board [terrain units]
  (GameBoard. terrain units))

(defn generate-game-board [game-map unit-spec]
  (let [terrain (:terrain game-map)
        units (zipmap (keys (:units game-map))
                      (map #(netwars.aw-unit/make-unit unit-spec (:id %) (:color %))
                           (vals (:units game-map))))]
    (make-game-board terrain units)))

(defn get-terrain [^GameBoard board coord]
  (at (:terrain board) coord))

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

(defn can-walk-on-field?
  "Returns true if unit can pass a field.
   Checks if the movement-type can pass the field and if an unit is on the field,
   it checks if both units have the same color."
  [board unit c]
  (and (or (nil? (get-unit board c)) (= (:color unit) (:color (get-unit board c))))
       (can-pass? (get-terrain board c) (:movement-type (meta unit)))
       ))

(defn move-unit [^GameBoard board c1 c2]
  {:pre [(not (nil? (get-unit board c1))) (nil? (get-unit board c2))]
   :post [(nil? (get-unit % c1)) (not (nil? (get-unit % c2)))]}
  (let [u (get-unit board c1)]
   (assoc board :units (-> (:units board) (assoc c2 u) (dissoc c1)))))

(defn change-building-color [^GameBoard board c color]
  {:pre [(is-building? (get-terrain board c))]
   :post [(= color (second (get-terrain % c)))]}
  (let [terrain (:terrain board)]
   (assoc board :terrain (update-board terrain c
                                       (assoc (get-terrain board c) 1 color)))))

;;; Attacking

(defn in-attack-range? [board att-coord vic-coord]
  (let [dist (distance att-coord vic-coord)
        att (get-unit board att-coord)
        def (get-unit board vic-coord)]
    (or (contains? (:range (main-weapon att)) dist)
        (contains? (:range (alt-weapon att)) dist))))

;;; Movement Range

(use '[clojure.set :as set])

(defn reachable-fields
  "Returns a set of all coordinates reachable by unit on coordinate c"
  [board c]
  (let [unit (get-unit board c)
        movement-range (:movement-range (meta unit))
        movement-type (:movement-type (meta unit))
        fuel (:fuel unit)]
    ;; (println "movement-type:" movement-type)
    ;; (println "movement-range:" movement-range)
    ;; (println "fuel:" fuel)
    ;; (println "unit:" unit)
    (letfn [(helper [c rest & {:keys [initial?]}]
              (let [t (get-terrain board c)
                    costs (if initial? 0 (movement-costs t movement-type))]
                ;; (println (str "[" (:x c) "," (:y c) "]") ";" t "costs:" costs)
               (cond
                (nil? c) #{}
                (not (in-bounds? (:terrain board) c)) #{}
                (not (can-pass? t movement-type)) #{}
                (and (not initial?) (not (can-walk-on-field? board unit c))) #{}
                (> rest costs) (set/union #{c}
                                            (helper (coord (inc (:x c)) (:y c))
                                                    (- rest costs))
                                            (helper (coord (dec (:x c)) (:y c))
                                                    (- rest costs))
                                            (helper (coord (:x c) (inc (:y c)))
                                                    (- rest costs))
                                            (helper (coord (:x c) (dec (:y c)))
                                                    (- rest costs)))
                true #{c})))]
      (helper c (min movement-range fuel) :initial? true))))

(comment
  (require 'netwars.aw-map.loader)
  (require 'netwars.aw-unit.loader)

  (let [loaded-map (netwars.aw-map.loader/load-map "maps/7330.aws")
        unit-spec (netwars.aw-unit.loader/load-units "resources/units.xml")
        terrain (:terrain loaded-map)
        units (zipmap (keys (:units loaded-map))
                      (map #(netwars.aw-unit/make-unit unit-spec (:id %) (:color %))
                           (vals (:units loaded-map))))
        testboard (make-game-board terrain units)]
    (defn dump-coords [c] (reachable-fields testboard c))

    (defn draw-reachables [c]
      (let [cs (reachable-fields testboard c)]
       (doseq [y (range 0 (height (:terrain testboard)))
               x (range 0 (width (:terrain testboard)))]
         (print (cond
                 (= c (coord x y)) " o"
                 (contains? cs (coord x y)) " x"
                 :true "  "))
         (when (= 0 (rem (inc x) (width (:terrain testboard))))
           (prn)))))))
