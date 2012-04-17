(ns netwars.path
  (:require [netwars.game-board :as board]
            [netwars.aw-map :as aw-map]))

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

(when clojure.core/print-method
  (defmethod clojure.core/print-method AwPath [o p]
   (.write p (.toString o))))

(defn path?
  "Predicate to test if an object is a path."
  [p]
  (instance? AwPath p))

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
  (AwPath. coords))

(defn get-coordinates
  "Returns the seq of coordinates of the path"
  [path]
  (:coordinates path))
