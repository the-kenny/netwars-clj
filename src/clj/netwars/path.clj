(ns netwars.path
  (:use [netwars.aw-map :only [distance]]
        [netwars.game-board :as board])
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
     (and (every? (partial instance? Coordinate) path)
          (every? #(= (apply distance %) 1) (partition 2 1 path))))
  ([path board]
     (and (valid-path? path)
          (get-unit board (first path))
          (not (get-unit board (last path)))
          ;; TODO: Test for real fuel-costs
          (< (count path) (let [u (get-unit board (first path))]
                            (min (:movement-range (meta u)) (:fuel u)))))))

(defn make-path [coords]
  ;; Validate integrity of `coords`
  (let [path (AwPath. coords)]
   (when-not (valid-path? path)
     (throw (java.lang.IllegalArgumentException.
             "`coords` wouldn't create a valid path object")))
   path))

(defn get-coordinates [path]
  (:coordinates path))
