(ns netwars.map-drawer
  (:use netwars.map-utils
        netwars.sprite-loader
        netwars.map-loader)
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
  (nth seq (+ y (* x height)) nil))

;;; Utility Functions

(defn- uldr-sorter [k]
  (condp = k
    :u 0
    :l 1
    :d 2
    :r 3
    4)
  )
(def +dir-mappings+
  {:north :u,
   :east :r
   :south :d
   :west :l})

(defn rename-direction [dir]
  (get +dir-mappings+ dir))

(defn stringify-directions [dirs]
  (keyword (apply str
                  (map name (sort-by uldr-sorter
                                     (map rename-direction dirs))))))

(defn- direction-complement [dir]
  (get {:north :south,
        :south :north,
        :east :west,
        :west :east}
       dir))

;;; Begin of section for orienting tiles

(defmacro def-orientation-method
  "An orientation method should return a list consisting of :u :l :d :r,
  according to the applicable directions where this tile can be connected to."
  [type [neighbours] & body]
  `(defmethod tile-orientation ~type [~'_ ~neighbours]
     (when-let [dirs# (do ~@body)]
       [~type (keyword (apply str
                         (map name (sort-by uldr-sorter
                                            (map rename-direction dirs#)))))])))

(defn get-connectables-directions
  "Returns an vector of directions ([:north :south etc.]) to where the
  binary-predicate connectable-fn returns true."
  [connectable-fn type neighbours]
  (reduce (fn [acc o] ;Get possible connections in all dirs
            (if (connectable-fn type (get neighbours o))
              (conj acc o)
              acc)) [] [:north :east :south :west]))

(defmacro def-straighten-orientation-method [type]
  `(def-orientation-method ~type [nbs#]
     (let [dirseq# (get-connectables-directions connectable? ~type nbs#)]
       ;; Streets have at least two endpoints (Special handling for :u and :d)
       (cond
        (or (= dirseq# [:north])
            (= dirseq# [:south])) [:north :south]
        (or (= dirseq# [:east])
            (= dirseq# [:west])
            (empty? dirseq#)) [:east :west]
        true dirseq#))))

(def-straighten-orientation-method :street)
(def-straighten-orientation-method :river)

(def-orientation-method :bridge [nbs]
  (if (or (connectable? :bridge (:north nbs))
          (connectable? :bridge (:south nbs)))
    [:north :south]
    [:east :west]))

(def-orientation-method :wreckage [nbs]
  (cond
   (or (connectable? :wreckage (:east nbs))
       (connectable? :wreckage (:west nbs))) [:east :west]
   (or (connectable? :wreckage (:north nbs))
       (connectable? :wreckage (:south nbs))) [:north :south]))

(defmethod tile-orientation :mountain [_ nbs]
  [:mountain (if (:north nbs)
               :big
               :small)])

;; (def-orientation-method :pipe [nbs]
;;   [:east :west])

(def-straighten-orientation-method :pipe)
(def-straighten-orientation-method :segment-pipe)

(def-orientation-method :beach [nbs] 
  (get-connectables-directions (fn [_ t] (is-ground? t)) :beach nbs))

(defmethod tile-orientation :water [_ nbs]
  (if-let [grounds (seq (get-connectables-directions
                         (fn [_ t] (is-ground? t))
                         :water nbs))] 
    [:seaside (stringify-directions grounds)]))

(defmethod tile-orientation :building [type _]
  type)

(defmethod tile-orientation :default [type _]
  [type nil])

;;; End of orientation-section

(defn orientate-terrain-tiles [map-struct]
  (for [x (range (:width map-struct))
        y (range (:height map-struct))
        :let [data (terrain-at map-struct x y)]] 
    (tile-orientation data (neighbours map-struct x y))))

(defn render-map-to-image [loaded-map]
  (let [image (BufferedImage. (* 16 (:width loaded-map))
                              (* 16 (:height loaded-map))
                              BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)
        oriented-tiles (orientate-terrain-tiles loaded-map)]
    (.drawImage graphics (load-pixmap "pixmaps/background.png") 0 0 nil)
    (doseq [x (range (:width loaded-map))
            y (range (:height loaded-map))]
      (when-let [ordt (get-coordinate oriented-tiles
                                      (:width loaded-map)
                                      (:height loaded-map)
                                      x y)]
        (if-let [tile #^BufferedImage (if (is-building? (first ordt))
                                        (load-building-tile ordt)
                                        (load-terrain-tile ordt))]
          (.drawImage graphics
                      tile
                      (- (* x 16) (- (.getWidth tile) 16))
                      (- (* y 16) (- (.getHeight tile) 16))
                      nil)
          (println ordt " not found."))))
    (.finalize graphics)
    image))

(defn- test-drawing [file]
  (let [m (netwars.map-loader/load-map "/Users/moritz/Development/clojure/netwars/bla.aws")]
    (javax.imageio.ImageIO/write (time (render-map-to-image m)) "png" (java.io.File. file))))

(comment (test-drawing "/Users/moritz/7330.png"))
