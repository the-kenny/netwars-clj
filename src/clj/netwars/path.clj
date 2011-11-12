(ns netwars.path
  (:use [netwars.aw-map :only [distance]]
        [netwars.game-board :as board]
        [netwars.aw-map :as aw-map])
  (:import netwars.aw_map.Coordinate))

(deftype AwPath [coordinates]
  Object
  (toString [this] (str "<AwPath [" (apply str (map (juxt :x :y) coordinates)) "]>"))
  clojure.lang.IPersistentMap
  clojure.lang.ILookup
  (valAt [this key] (when (= key :coordinates) coordinates))
  (valAt [this key notfound] (if (= key :coordinates) coordinates notfound))
  clojure.lang.IPersistentCollection
  (count [this] (.count coordinates))
  (empty [this]  (AwPath. nil))
  (cons [this e]  (AwPath. (.cons coordinates e)))
  (equiv [this gs] (or (identical? this gs)
                       (when (identical? (class this) (class gs))
                         (= coordinates (.coordinates gs)))))
  clojure.lang.Seqable
  (seq [this] (seq coordinates)))

(defmethod clojure.core/print-method AwPath [o p]
  (.write p (.toString o)))


(defn valid-path?
  "Checks if a path is valid.
With the optional second argument, it checks for validity in the context of the given board."
  ([path]
     (and (>= (count path) 2)
          (every? (partial instance? Coordinate) path)
          ;; Check if euclidean-distance doesn't exceed 1
          (every? #(= (apply distance %) 1) (partition 2 1 path))
          (= (count path) (count (set path))) ;naive check for duplicates
          ))
  ([path board]
     (and (valid-path? path)
          (get-unit board (first path))
          (not (get-unit board (last path)))
          ;; TODO: Test for real fuel-costs
          (< (count path) (let [u (get-unit board (first path))]
                            (min (:movement-range (meta u)) (:fuel u))))
          (every? #(aw-map/can-pass? (get-terrain board %1)
                                     (:movement-type (meta (get-unit board
                                                                     (first path)))))
                  path))))

(defn make-path [coords]
  (AwPath. coords))

(defn get-coordinates [path]
  (:coordinates path))
