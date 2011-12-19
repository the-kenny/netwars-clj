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

(defn path? [p]
  (instance? AwPath p))

(defn valid-path?
  "Checks if a path is valid.
With the optional second argument, it checks for validity in the context of the given board."
  ([path]
     (and (>= (count path) 2)
          (every? #(instance? Coordinate %) path)
          ;; Check if euclidean-distance doesn't exceed 1
          (every? #(= (apply distance %) 1) (partition 2 1 path))
          (= (count path) (count (set path))) ;naive check for duplicates
          ))
  ([path board]
     (let [unit (get-unit board (first path))]
       (and (valid-path? path)
            (get-unit board (first path))
            (not (get-unit board (last path)))
            (<= (count (rest path))
                (min (:movement-range (meta unit)) (:fuel unit)))
            (every? #(aw-map/can-pass? (get-terrain board %)
                                 (:movement-type (meta unit)))
              path)))))

(defn make-path [coords]
  (AwPath. coords))

(defn get-coordinates [path]
  (:coordinates path))
