(ns netwars.path
  (:use [netwars.aw-map :only [distance]])
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


(defn valid-path? [path]
  (and (every? (partial instance? Coordinate) path)
       (every? #(= (apply distance %) 1) (partition 2 1 path))))

(defn make-path [coords]
  ;; Validate integrity of `coords`
  (let [path (AwPath. coords)]
   (when-not (valid-path? path)
     (throw (java.lang.IllegalArgumentException.
             "`coords` doesn'wouldn't create a valid path object")))
   path))

(defn get-coordinates [path]
  (:coordinates path))
