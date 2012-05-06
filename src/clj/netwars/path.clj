(ns netwars.path
  (:require [netwars.game-board :as board]
            [netwars.aw-map :as aw-map]))

(defn path?
  "Predicate to test if an object is a path."
  [p]
  (and (vector? p)
       (every? aw-map/coord? p)))

(defn valid-path?
  "Checks if a path is valid.
With the optional second argument, it checks for validity in the context of the given board."
  ([path]
     (and (>= (count path) 2)
          (every? aw-map/coord? path)
          ;; Check if euclidean-distance doesn't exceed 1
          (every? #(= (apply aw-map/distance %) 1) (partition 2 1 path))
          (= (count path) (count (set path))) ;naive check for duplicates
          ))
  ([path board]
     (let [unit (board/get-unit board (first path))]
       (and (valid-path? path)
            (board/get-unit board (first path))
            (not (board/get-unit board (last path)))
            (<= (count (rest path))
                (min (:movement-range (meta unit)) (:fuel unit)))
            (every? #(aw-map/can-pass? (board/get-terrain board %)
                                       (:movement-type (meta unit)))
              path)))))

(defn make-path
  "Creates a path from a list of coordinates"
  [coords]
  (vec coords))
