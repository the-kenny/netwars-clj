(ns netwars.map-drawer
  (:use netwars.aw-map
        netwars.map-utils
        netwars.map-loader
        [clojure.java.io :only [resource]])
  (:require [netwars.utilities :as util])
  (:import java.awt.Graphics2D
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))

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

(defmulti draw-tile
  "A method which returns the direction for the specific tile
Takes three arguments: the type, the neighbours and a drawing-function.
drawing-fn takes a vector as the only argument, defining the path to the tile drawn.

For example: [:pipe :uldr] or [:seaside :corner :dr]"
  dispatch-fn)

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
  `(defmethod draw-tile ~type [~'_ ~neighbours drawing-fn#]
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
(defmethod draw-tile :wreckage [_ nbs drawing-fn]
  (if-let [dirs (cond
                 (or (connectable? :wreckage (:east nbs))
                     (connectable? :wreckage (:west nbs))) [:east :west]
                 (or (connectable? :wreckage (:north nbs))
                     (connectable? :wreckage (:south nbs))) [:north :south])]
    (drawing-fn [:wreckage (stringify-directions dirs)])
    (drawing-fn [:wreckage])))

(defmethod draw-tile :mountain [_ nbs drawing-fn]
           (drawing-fn [:mountain (if (:north nbs)
                                    :big
                                    :small)]))

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
   (map #(keyword (apply str %)) (.split (name inter-dir) "-")))

(defn- seaside-corners [nbs drawing-fn]
  (doseq [inter-dir [:north-east :south-east :north-west :south-west]]
    (let [dirs (split-intercardinal-direction inter-dir)]
      (when (and (is-ground? (get nbs inter-dir))
                 (every? #(or (is-water? %)
                              (= % :beach)) (map #(get nbs %) dirs)))
        (drawing-fn [:seaside :corner (stringify-directions dirs)])))))

(defmethod draw-tile :water [_ nbs drawing-fn]
  (do
   (seaside nbs drawing-fn)
   (seaside-corners nbs drawing-fn)
   (river-mouth nbs drawing-fn)))

(defmethod draw-tile :building [type _ drawing-fn]
           (drawing-fn (cons :buildings type)))

(defmethod draw-tile :default [type _ drawing-fn]
  (drawing-fn [(name type)]))

;;; End of orientation-section

(let [tile-cache (atom {})]
  (defn- load-pixmap [#^String file]
    (if-let [img (get @tile-cache file)]
      img
      (if-let [res (resource file)]
        (get (swap! tile-cache assoc file (ImageIO/read res))
             file)))))

(defn load-terrain-tile
  "Loads a terrain tile from pixmaps/grounds.
  Every segment is a keyword describing a level in the filesystem structure:
  street/lr.png would be [:street :lr]
  If a segment is nil, it will be ignored."
  [[& segments]]
  (load-pixmap (str (reduce #(when %2 (str %1 "/" (name %2)))
                            "pixmaps/ground"
                            (remove nil? segments))
                    ".png")))

(defn load-building-tile [[building color]]
  (load-pixmap (str "pixmaps/ground/buildings/"
                    (name building)
                    "/"
                    (name color)
                    ".png")))

(defn draw-img [graphics c tile]
  (.drawImage graphics
              tile
              (- (* (:x c) 16) (- (.getWidth tile) 16))
              (- (* (:y c) 16) (- (.getHeight tile) 16))
              nil))

(defn drawing-fn [graphics c path]
  (if-let [tile #^BufferedImage (if (is-building? (first path))
                                  (load-building-tile path)
                                  (load-terrain-tile path))]
    (draw-img graphics c tile)
    (println path " not found.")))

(defn render-terrain-board [terrain-board]
  (let [image (BufferedImage. (* 16 (width terrain-board))
                              (* 16 (height terrain-board))
                              BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)]
    (.drawImage graphics (load-pixmap "pixmaps/background.png") 0 0 nil)
    (doseq [x (range (width terrain-board))
            y (range (height terrain-board))
            :let [c (coord x y)]]
      (when-let [terr (at terrain-board c)]
        (draw-tile terr (neighbours terrain-board c)
                   (partial drawing-fn graphics c))))
    (.finalize graphics)
    image))

;; (defn render-map-to-image [loaded-map]
;;   (let [terrain (:terrain loaded-map)
;;         image (BufferedImage. (* 16 (width terrain))
;;                               (* 16 (height terrain))
;;                               BufferedImage/TYPE_INT_ARGB)
;;         graphics (.createGraphics image)]
;;     (.drawImage graphics (load-pixmap "pixmaps/background.png") 0 0 nil)
;;     (doseq [x (range (width terrain))
;;             y (range (height terrain))
;;             c (coord x y)]
;;       (when-let [terr (at terrain c)]
;;         (draw-tile terr (neighbours loaded-map c)
;;                           (partial drawing-fn graphics c))))
;;     (.finalize graphics)
;;     image))

;; (defn render-path-to-map [map-img path]
;;   (let [lst (:path path)
;;         img (load-pixmap "pixmaps/misc/path.png")
;;         graphics (.createGraphics map-img)]
;;     (doseq [[x y _] lst]
;;       (println x y)
;;       (draw-img graphics c img)))
;;   map-img)

