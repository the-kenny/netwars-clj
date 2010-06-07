(ns netwars.map-drawer
  (:use netwars.map-utils
        netwars.sprite-loader
        netwars.map-loader)
  (:require [clojure.contrib.str-utils2 :as str2])
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
  (or (= t2 :bridge) (and (is-ground? t2) (not= t2 :river))))

;; (defmethod connectable? :street [_ t2]
;;   (boolean (#{:street :bridge} t2)))

(defn- dispatch-fn [terrain _ _]
  ;; Buildings are [type color] terrains just type
  (if (vector? terrain)           
    :building
    terrain))

(defmulti tile-orientation
  "A method which returns the direction for the specific tile
Takes three arguments: the type, the neighbours and a drawing-function.
drawing-fn takes a vector as the only argument, defining the path to the tile drawn.

For example: [:pipe :uldr] or [:seaside :corner :dr]"
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
    4))

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
  `(defmethod tile-orientation ~type [~'_ ~neighbours drawing-fn#]
     (when-let [dirs# (do ~@body)]
       (drawing-fn# 
        [~type (stringify-directions dirs#)]))))

(defn get-connectables-directions
  "Returns an vector of directions ([:north :south etc.]) to where the
  binary-predicate connectable-fn returns true."
  [connectable-fn type neighbours]
  (reduce (fn [acc o] ;Get possible connections in all dirs
            (if (connectable-fn type (get neighbours o))
              (conj acc o) acc))
          [] [:north :east :south :west]))

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

;;; :wreckage has a special-handling for a single wreckage, w/o a direction
(defmethod tile-orientation :wreckage [_ nbs drawing-fn]
  (if-let [dirs (cond
                 (or (connectable? :wreckage (:east nbs))
                     (connectable? :wreckage (:west nbs))) [:east :west]
                 (or (connectable? :wreckage (:north nbs))
                     (connectable? :wreckage (:south nbs))) [:north :south])]
    (drawing-fn [:wreckage (stringify-directions dirs)])
    (drawing-fn [:wreckage])))

(defmethod tile-orientation :mountain [_ nbs drawing-fn]
           (drawing-fn [:mountain (if (:north nbs)
                                    :big
                                    :small)]))

;; (def-orientation-method :pipe [nbs]
;;   [:east :west])

(def-straighten-orientation-method :pipe)
(def-straighten-orientation-method :segment-pipe)

(def-orientation-method :beach [nbs] 
  (get-connectables-directions (fn [_ t] (is-ground? t)) :beach nbs))

(defmacro river-mouth-cond [dir nbs]
  `(and (= :river (~dir ~nbs))
        (every? is-water?
                (vals (select-keys ~nbs (rectangular-direction ~dir))))))

(defmacro river-mouth-cond [dir nbs]
  `(and (= :river (~dir ~nbs))
        (every? is-water?
                (vals  (drop-neighbours-behind
                        (direction-complement ~dir)
                        ~nbs)))))

(defn- river-mouth [nbs drawing-fn]
  (when-let [dir (cond
                  (river-mouth-cond :north nbs) :north
                  (river-mouth-cond :south nbs) :south
                  (river-mouth-cond :east nbs) :east
                  (river-mouth-cond :west nbs) :west)]
        (drawing-fn [:river :mouth (stringify-directions [dir])])))

(defn- seaside [nbs drawing-fn]
  (when-let [grounds (seq (get-connectables-directions
                           (fn [_ t] (is-ground? t))
                           :water nbs))]
    (drawing-fn [:seaside (stringify-directions grounds)])))

(defn split-intercardinal-direction [inter-dir]
   (map #(keyword (apply str %)) (str2/split (name inter-dir) #"-")))

(defn- seaside-corners [nbs drawing-fn]
  (doseq [inter-dir [:north-east :south-east :north-west :south-west]]
    (let [dirs (split-intercardinal-direction inter-dir)]
      (when (and (is-ground? (get nbs inter-dir))
                 (every? #(or (is-water? %)
                              (= % :beach)) (map #(get nbs %) dirs)))
        (drawing-fn [:seaside :corner (stringify-directions dirs)])))))

(defmethod tile-orientation :water [_ nbs drawing-fn]
  (do 
   (seaside nbs drawing-fn)
   (seaside-corners nbs drawing-fn)
   (river-mouth nbs drawing-fn)))

(defmethod tile-orientation :building [type _ drawing-fn]
           (drawing-fn (cons :buildings type)))

(defmethod tile-orientation :default [type _ drawing-fn]
  (drawing-fn [(name type)]))

;;; End of orientation-section

(defn drawing-fn [graphics x y path]
  (if-let [tile #^BufferedImage (if (is-building? (first path))
                                  (load-building-tile path)
                                  (load-terrain-tile path))]
    (.drawImage graphics
                tile
                (- (* x 16) (- (.getWidth tile) 16))
                (- (* y 16) (- (.getHeight tile) 16))
                nil)
    (println path " not found.")))

(defn render-map-to-image [loaded-map]
  (let [image (BufferedImage. (* 16 (:width loaded-map))
                              (* 16 (:height loaded-map))
                              BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)]
    (.drawImage graphics (load-pixmap "pixmaps/background.png") 0 0 nil)
    (doseq [x (range (:width loaded-map))
            y (range (:height loaded-map))]
      (when-let [terr (terrain-at loaded-map x y)]
        (tile-orientation terr (neighbours loaded-map x y)
                          (partial drawing-fn graphics x y))))
    (.finalize graphics)
    image))

(defn- test-drawing [file]
  (let [m (netwars.map-loader/load-map "/Users/moritz/Development/clojure/netwars/bla.aws")]
    (javax.imageio.ImageIO/write (time (render-map-to-image m)) "png" (java.io.File. file))))

(comment (test-drawing "/Users/moritz/7330.png"))
