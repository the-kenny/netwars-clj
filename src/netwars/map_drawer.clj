(ns netwars.map-drawer
  (:use netwars.map-utils))

;; (defn tile-orientation [map-struct x y]
;;   (let [{:keys [north east south west
;;                 north-east north-west
;;                 south-east south-west]}
;;         (neighbours map-struct x y)]

;;     fun-stuff))

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
  (or (= t2 :bridge) (and (is-ground t2) (not= t2 :river))))

;; (defmethod connectable? :street [_ t2]
;;   (boolean (#{:street :bridge} t2)))

(defn- dispatch-fn [terrain _]
  (if (vector? terrain)
    :building
    terrain))

(defmulti tile-orientation
  "A method which returns the direction for the specific tile"
  dispatch-fn)

;; (defmacro def-orientation-method [type & body]
;;   `(defmethod tile-orientation ~type [terrain neighbours]
;;      (let [{:keys ~'[north east south west
;;                      north-east north-west
;;                      south-east south-west]}
;;            ~neighbours]
;;        ~@body)))

;; (def-orientation-method :building
;;   nil)

(defn uldr-sorter [k]
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

(defmacro def-orientation-method
"An orientation method should return a list consisting of :u :l :d :r,
according to the applicable directions where this tile can be connected to."
  [type [neighbours] & body]
  `(defmethod tile-orientation ~type [~'_ ~neighbours]
     (keyword (apply str
                     (map name (sort-by uldr-sorter
                                        (replace +dir-mappings+
                                                 (do ~@body))))))))

(def-orientation-method :street [nbs]
  (reduce (fn [acc o]
            (if (connectable? :street (o nbs))
              (conj acc o)
              acc)) [] [:north :east :west :south]))


(defmethod tile-orientation :building [_ _]
  nil)

(defmethod tile-orientation :default [type _]
  nil)
