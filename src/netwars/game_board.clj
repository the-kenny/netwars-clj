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

(defn remove-unit [^GameBoard board coord]
  {:pre [(not (nil? (get-unit board coord)))]
   :post [(nil? (get-unit % coord))]}
  (assoc board :units (dissoc (:units board) coord)))

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

(comment
  (require netwars.map-loader
           netwars.unit-loader
           netwars.aw-unit)
  (let [loaded-map (netwars.map-loader/load-map "maps/7330.aws")
        unit-spec (netwars.unit-loader/load-units "resources/units.xml")
        terrain (:terrain loaded-map)
        units (zipmap (keys (:units loaded-map))
                      (map #(netwars.aw-unit/make-unit unit-spec (:id %) (:color %))
                           (vals (:units loaded-map))))]
   (def +board+ (make-game-board terrain units))))
