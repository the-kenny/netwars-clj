(ns netwars.map-utils
  (:use netwars.map-loader)
  (:require clojure.inspector))

(defn on-map [map-struct x y]
  (and (< (* x y) (* (:width map-struct) (:height map-struct)))
       (>= x 0)
       (>= y 0)
       (< x (:width map-struct))
       (< y (:height map-struct))))

(defn terrain-at [map-struct x y]
  (nth (:terrain-data map-struct) (+ y (* x (:height map-struct))) nil))


(defn neighbours [map-struct x y]
  (let [msta (partial terrain-at map-struct)]
   (hash-map
    :north (msta x (dec y))
    :east (msta (inc x) y)
    :south (msta x (inc y))
    :west (msta (dec x) y)

    :north-east (msta (inc x) (dec y))
    :south-east (msta (inc x) (inc y))
    :north-west (msta (dec x) (dec y))
    :south-west (msta (dec x) (inc y)))))

(defn rectangular-direction [dir]
  (get {:north [:east :west]
        :south [:east :west]
        :east [:north :south]
        :west [:north :south]} dir nil))

(defn drop-neighbours-behind [direction nbs]
  (select-keys nbs
               (condp = direction
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

(defn inspect-terrain [loaded-map]
  (clojure.inspector/inspect
   (apply merge (for [x (range 30) y (range 20)]
                  {[x y] (terrain-at loaded-map x y)}))))

;; (defn is-ground [terrain]
;;   (boolean (and terrain
;;                 (not (#{:water :reef :bridge :beach} terrain)))))
