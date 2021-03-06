(ns netwars.map-renderer
  (:require [netwars.aw-map :as aw-map]
            [netwars.map-utils :as map-utils])
  (:require;*CLJSBUILD-REMOVE*;-macros
   [netwars.map-renderer-macro-hack :as macros]))

(defmulti connectable? (fn [t1 _] t1))

;; (defmacro defconnectable [t1 ts]
;;   `(defmethod connectable? ~t1 [_# t2#]
;;      (boolean (#{~@ts} t2#))))

(macros/defconnectable :street [:street :bridge])
(macros/defconnectable :pipe [:pipe :segment-pipe :wreckage :base])
(macros/defconnectable :segment-pipe [:pipe :segment-pipe :wreckage :base])
(macros/defconnectable :wreckage [:pipe :segment-pipe])
(macros/defconnectable :river [:river :bridge])

;;; Handle bridges, which are special in case of rivers
(defmethod connectable? :bridge [_ t2]
  (or (= t2 :bridge) (and (aw-map/is-ground? t2) (not= t2 :river))))

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
  (case k
    :u 0
    :l 1
    :d 2
    :r 3
    4))

(defn rename-direction [dir]
  (case dir
    :north :u
    :east  :r
    :south :d
    :west  :l))

(defn stringify-directions [dirs]
  (keyword (apply str
                  (map name (sort-by uldr-sorter
                                     (map rename-direction dirs))))))

(defn- direction-complement [dir]
  (case dir
    :north :south
    :south :north
    :east  :west
    :west  :east))

;;; Begin of section for orienting tiles

;; (defmacro def-orientation-method
;;   "An orientation method should return a list consisting of :u :l :d :r,
;;   according to the applicable directions where this tile can be connected to."
;;   [type [neighbours] & body]
;;   `(defmethod draw-tile ~type [~'_ ~neighbours drawing-fn#]
;;      (when-let [dirs# (do ~@body)]
;;        (drawing-fn#
;;         [~type (stringify-directions dirs#)]))))

(defn get-connectables-directions
  "Returns an vector of directions ([:north :south etc.]) to where the
  binary-predicate connectable-fn returns true."
  [connectable-fn type neighbours]
  (reduce (fn [acc o] ;Get possible connections in all dirs
            (if (connectable-fn type (get neighbours o))
              (conj acc o) acc))
          [] [:north :east :south :west]))

;; (defmacro def-straighten-orientation-method [type]
;;   `(def-orientation-method ~type [nbs#]
;;      (let [dirseq# (get-connectables-directions connectable? ~type nbs#)]
;;        ;; Streets have at least two endpoints (Special handling for :u and :d)
;;        (cond
;;         (or (= dirseq# [:north])
;;             (= dirseq# [:south])) [:north :south]
;;         (or (= dirseq# [:east])
;;             (= dirseq# [:west])
;;             (empty? dirseq#)) [:east :west]
;;         true dirseq#))))

(macros/def-straighten-orientation-method :street)
(macros/def-straighten-orientation-method :river)

(macros/def-orientation-method :bridge [nbs]
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

(macros/def-straighten-orientation-method :pipe)
(macros/def-straighten-orientation-method :segment-pipe)

(macros/def-orientation-method :beach [nbs]
  (get-connectables-directions (fn [_ t] (aw-map/is-ground? t)) :beach nbs))

;; (defmacro river-mouth-cond [dir nbs]
;;   `(and (= :river (~dir ~nbs))
;;         (every? aw-map/is-water?
;;                 (vals (select-keys ~nbs (map-utils/rectangular-direction ~dir))))))

;; (defmacro river-mouth-cond [dir nbs]
;;   `(and (= :river (~dir ~nbs))
;;         (every? aw-map/is-water?
;;                 (vals  (map-utils/drop-neighbours-behind
;;                         (direction-complement ~dir)
;;                         ~nbs)))))

(defn- river-mouth [nbs drawing-fn]
  (when-let [dir (cond
                  (macros/river-mouth-cond :north nbs) :north
                  (macros/river-mouth-cond :south nbs) :south
                  (macros/river-mouth-cond :east nbs) :east
                  (macros/river-mouth-cond :west nbs) :west)]
        (drawing-fn [:river :mouth (stringify-directions [dir])])))

(defn- seaside [nbs drawing-fn]
  (when-let [grounds (seq (get-connectables-directions
                           (fn [_ t] (aw-map/is-ground? t))
                           :water nbs))]
    (drawing-fn [:seaside (stringify-directions grounds)])))

(defn split-intercardinal-direction [inter-dir]
   (map #(keyword (apply str %)) (.split (name inter-dir) "-")))

(defn- seaside-corners [nbs drawing-fn]
  (doseq [inter-dir [:north-east :south-east :north-west :south-west]]
    (let [dirs (split-intercardinal-direction inter-dir)]
      (when (and (aw-map/is-ground? (get nbs inter-dir))
                 (every? #(or (aw-map/is-water? %)
                              (= % :beach)) (map #(get nbs %) dirs)))
        (drawing-fn [:seaside :corner (stringify-directions dirs)])))))

(defmethod draw-tile :water [_ nbs drawing-fn]
  (do
    (drawing-fn [:water])
   (seaside nbs drawing-fn)
   (seaside-corners nbs drawing-fn)
   (river-mouth nbs drawing-fn)))

(defmethod draw-tile :building [type _ drawing-fn]
           (drawing-fn (cons :buildings type)))

(defmethod draw-tile :default [type _ drawing-fn]
  (drawing-fn [(keyword type)]))

;;; End of orientation-section
