(ns netwars.pathfinding
  (:require [netwars.aw-map :as aw-map]))

;;; A path is a mutable data structure holding a ordered sequence of coordinates.
;;; Updates using update-path! are mutable.
;;; The data structure can't be used as a normal seq. The sequence of elements is accessable via `elements`

(defn make-path [start]
  (atom [start]))

(defn append! [path c]
  (swap! path conj c))

(defn elements [path]
  @path)

(defn start [path]
  (first (elements path)))

(defn end [path]
  (last (elements path)))

(defn- length [path]
  (count (elements path)))

(defn in-path? [path c]
  (contains? (set (elements path)) c))

(defn update-path! [path movement-range c max-length]
  (cond
   ;; Element is right next to the path and not already in path. Add it.
   (and (contains? movement-range c)
        (not (in-path? path c))
        (= 1 (aw-map/distance (aw-map/coord (end path))
                              (aw-map/coord c)))
        (if max-length (<= (length path) max-length) true))
   (append! path c)

   ;; Element is already in path. Shorten it.
   (in-path? path c)
   (reset! path (conj (vec (take-while #(not= c %) (elements path))) c))))
