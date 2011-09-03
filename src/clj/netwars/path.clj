(ns netwars.path)

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


(defn make-path [coords]
  (AwPath. coords))

(defn get-coordinates [path]
  (:coordinates path))

