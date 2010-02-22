(ns netwars.map-utils
  (:use netwars.map-loader)
  (:require clojure.inspector))

(defn on-map [map-struct x y]
  (and (< (* x y) (* (:width map-struct) (:height map-struct)))
       (>= x 0)
       (>= y 0)
       (< x (:width map-struct))
       (< y (:height map-struct))))

(defn tile-at [map-struct x y]
  (when (on-map map-struct x y)
    (nth (:terrain-data map-struct)
         (+ y (* x (:height map-struct))))))

(defn neighbours [map-struct x y]
  (let [msta (partial tile-at map-struct)]
   (hash-map
    :north (msta x (dec y))
    :east (msta (inc y) y)
    :south (msta x (inc y))
    :west (msta (dec x) y)

    :north-east (msta (inc x) (dec y))
    :south-east (msta (inc x) (inc y))
    :north-west (msta (dec x) (dec y))
    :south-west (msta (dec x) (inc y)))))

(defn inspect-terrain [loaded-map]
  (clojure.inspector/inspect
   (apply merge (for [x (range 30) y (range 20)]
                  {[x y] (tile-at loaded-map x y)}))))
