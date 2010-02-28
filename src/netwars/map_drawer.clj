(ns netwars.map-drawer
  (:use netwars.map-utils
        netwars.sprite-loader)
  (:import java.awt.Graphics2D
           java.awt.image.BufferedImage))

;; (defn tile-orientation [map-struct x y]
;;   (let [{:keys [north east south west
;;                 north-east north-west
;;                 south-east south-west]}
;;         (neighbours map-struct x y)]

;;     fun-stuff))

(defmulti connectable? (fn [t1 _] t1))

(defmacro defconnectable [t1 ts]
  `(defmethod connectable? ~t1 [_# t2#]
     (boolean (#{~@ts} t2#))))

(defconnectable :street [:street :bridge])
(defconnectable :pipe [:pipe :segment-pipe :wreckage :base])
(defconnectable :segment-pipe [:pipe :segment-pipe :wreckage :base])
(defconnectable :wreckage [:pipe :segment-pipe])
(defconnectable :river [:river :bridge])

;;; Handle bridges, which are special in case of rivers
(defmethod connectable? :bridge [_ t2]
  (or (= t2 :bridge) (and (is-ground t2) (not= t2 :river))))

;; (defmethod connectable? :street [_ t2]
;;   (boolean (#{:street :bridge} t2)))

(defn- dispatch-fn [terrain _]
  (if (vector? terrain)
    :building
    terrain))

(defmulti tile-orientation
  "A method which returns the direction for the specific tile"
  dispatch-fn)

(defn- get-coordinate [seq width height x y]
  (when (and (< (count seq) (* x y))
             (< x width)
             (< y height))
   (nth seq (+ y (* x height)))))

(defn- uldr-sorter [k]
  (condp = k
    :u 0
    :l 1
    :d 2
    :r 3
    4))

(def +dir-mappings+
  {:north :u,
   :east :r
   :south :d
   :west :l})

(defmacro def-orientation-method
"An orientation method should return a list consisting of :u :l :d :r,
according to the applicable directions where this tile can be connected to."
  [type [neighbours] & body]
  `(defmethod tile-orientation ~type [~'_ ~neighbours]
     (keyword (apply str
                     (map name (sort-by uldr-sorter
                                        (replace +dir-mappings+
                                                 (do ~@body))))))))

(defn- direction-complement [dir]
  (get {:north :south,
        :south :north,
        :east :west,
        :west :east}
       dir))

(def-orientation-method :street [nbs]
  (let [dirseq (reduce (fn [acc o]      ;Get possible connections in all dirs
                         (if (connectable? :street (o nbs))
                           (conj acc o)
                           acc)) [] [:north :east :south :west])]
    ;; Streets have at least two endpoints (Special handling for :u and :d)
    (cond
     (or (= dirseq [:north])
         (= dirseq [:south])) [:north :south]
     (or (= dirseq [:east])
         (= dirseq [:west])) [:east :west]
     true dirseq)))

(defmethod tile-orientation :building [_ _]
  nil)

(defmethod tile-orientation :default [type _]
  nil)

(defn orientate-terrain-tiles [map-struct]
  (for [x (range (:width map-struct))
        y (range (:height map-struct))
        :let [data (terrain-at map-struct x y)]]
   (if-let [or (tile-orientation data (neighbours map-struct  x y))]
     [data or]
     data)))

(defn render-map-to-image [loaded-map]
  (let [image (BufferedImage. 320 240 BufferedImage/TYPE_INT_ARGB )
        graphics (.createGraphics image)
        oriented-tiles (orientate-terrain-tiles loaded-map)]
    (doseq [x (range (:width loaded-map))
            y (range (:height loaded-map))]
      (when-let [ordt (get-coordinate  oriented-tiles
                                          (:width loaded-map)
                                          (:height loaded-map)
                                          x y)]
        (when (and (sequential? ordt)
                   (not (#{:red :green :blue :yellow :black} (second ordt))))
          (.drawImage graphics
                      (load-terrain-tile ordt)
                      (* x 16)
                      (* y 16)
                      nil))))
    (.finalize graphics)
    image))
