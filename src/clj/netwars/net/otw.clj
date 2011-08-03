(ns netwars.net.otw
  (:import [clojure.lang IPersistentMap IPersistentVector Keyword]
           [java.util List UUID]
           [netwars.aw_map Coordinate]))

(defprotocol Sendable
  (encode [o]))

(extend-protocol Sendable
  Coordinate
  (encode [c] (with-meta [(:x c) (:y c)]
                {:otw-type :Coordinate}))
  List
  (encode [v] (map encode v))
  IPersistentMap
  (encode [m] (into {} (for [[k v] m] [(encode k) (encode v)])))
  UUID
  (encode [u] (str u))
  Object
  (encode [o] o))

(defn encode-data [data]
  (binding [*print-meta* true]
    (pr-str (encode data))))

(defn decode-data [s]
  (into {} (for [[k v] (read-string s)] [(keyword k) v])))
