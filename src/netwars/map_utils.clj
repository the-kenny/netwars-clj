(ns netwars.map-utils
  (:require [netwars.aw-map :as aw-map]))

;;; TODO: Simplify
(defn neighbours [terrain c]
  (let [msta #(aw-map/at terrain (aw-map/coord %1 %2))
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
  "Returns the cardinal directions 90 degrees and -90 degrees to the given direction"
  [dir]
  (get {:north [:east :west]
        :south [:east :west]
        :east [:north :south]
        :west [:north :south]} dir nil))

(defn drop-neighbours-behind
  "Drops the cardinal and intercardinal directions from `nbs` opposite to `direction`"
  [direction nbs]
  (select-keys nbs (get {:north [:north
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
                                 :east :west]}
                        direction)))
