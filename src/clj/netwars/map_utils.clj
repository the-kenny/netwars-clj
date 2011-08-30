(ns netwars.map-utils
  (:use netwars.map-loader
        [netwars.aw-map :only [at coord]])
  (:require clojure.inspector))

;;; TODO: Simplify
(defn neighbours [terrain c]
  (let [msta #(at terrain (coord %1 %2))
        x (:x c) y (:y c)]
   (hash-map
    :north (msta x (dec y))
    :east (msta (inc x) y)
    :south (msta x (inc y))
    :west (msta (dec x) y)

    :north-east (msta (inc x) (dec y))
    :south-east (msta (inc x) (inc y))
    :north-west (msta (dec x) (dec y))
    :south-west (msta (dec x) (inc y)))))

(defn rectangular-direction
  "Returns the directions 90 degrees and -90 degrees to the given direction"
  [dir]
  (get {:north [:east :west]
        :south [:east :west]
        :east [:north :south]
        :west [:north :south]} dir nil))

(defn drop-neighbours-behind [direction nbs]
  (select-keys nbs (case [direction]
                     :north [:north
                             :north-west :north-east
                             :east :west]
                     :east [:east
                            :north-east :south-east
                            :north :south]
                     :west [:west
                            :north-west :north-west
                            :north :south]
                     :south [:south
                             :south-west :south-east
                             :east :west])))
