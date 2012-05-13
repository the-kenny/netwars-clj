(ns netwars.pathfinding
  (:require [netwars.logging :as logging]
            [netwars.pathfinding.a-star :as a-star]
            [netwars.aw-map :as aw-map]
            [netwars.path :as aw-path]
            [clojure.set :as set]
            [clojure.string :as string]
            [netwars.logging :as logging]
            [dijkstra :as dijkstra]))

;;; A path is a mutable data structure holding a ordered sequence of coordinates.
;;; Updates using update-path! are mutable.
;;; The data structure can't be used as a normal seq. The sequence of elements is accessable via `elements`

;;; General path functions

(defn make-path [start]
  (atom [start]))

(defn- append! [path c]
  (swap! path conj c))

(defn elements [path]
  @path)

(defn- copy-path! [path source]
  (reset! path (vec (elements source))))

(defn- start [path]
  (first (elements path)))

(defn- end [path]
  (peek (elements path))                ;peek is faster than last on vectors
  )

(defn- length [path]
  (count (elements path)))

(defn in-path? [path c]
  (some #{c} (elements path)))

(declare shortest-path)

;;; TODO: Use netwars.path here
(defn path->aw-path [p]
  (aw-path/make-path (elements p)))

(defn update-path!
  "This function should be called when the mouse touches a new field while pathfinding.
It destructively updates the path according to some magic rules which implement nice pathfinding."
  [path movement-range c board unit]
  (let [terrain-board (:terrain board)
        max-length (min (:fuel unit)
                        (:movement-range (meta unit)))]
    (cond
     ;; Element is already in path. Shorten it.
     (in-path? path c)
     (reset! path
             #_(conj (vec (take-while #(not= c %) (elements path))) c)
             (let [els (elements path)]
               (subvec els 0 (inc (count (take-while #(not= c %) els))))))

     ;; Element is right next to the path and not already in path. Add it.
     (and (contains? movement-range c)
          (not (in-path? path c))
          (= 1 (aw-map/distance c (end path)))
          (if max-length (<= (length path) max-length) true))
     (let [shortest (shortest-path (start path) c movement-range board unit)]
       (if (< (length shortest) (length path))
         (copy-path! path shortest)
         (append! path c)))

     ;; If everything else fails, re-calculate using shortest-path
     (contains? movement-range c)
     (copy-path! path (shortest-path (start path) c movement-range board unit)))))

(declare dijkstra-wrapper)

(defn shortest-path [start end movement-range board unit]
  (let [movement-type (:movement-type (meta unit))]
   (atom (a-star/a-star-path board movement-range start end))))
