(ns netwars.pathfinding
  (:require [netwars.aw-map :as aw-map]
            [netwars.drawing :as drawing]))

;;; A path is a mutable data structure holding a ordered sequence of coordinates.
;;; Updates using update-path! are mutable.
;;; The data structure can't be used as a normal seq. The sequence of elements is accessable via `elements`

(defn make-path [start]
  (atom [start]))

(defn update-path! [path c]
  (swap! path conj c))

(defn elements [path]
  @path)

(defn start [path]
  (first (elements path)))

(defn end [path]
  (last (elements path)))
