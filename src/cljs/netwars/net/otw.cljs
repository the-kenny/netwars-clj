(ns netwars.net.otw
  (:require [netwars.aw-map :as aw-map]
            [cljs.reader :as reader]
            [netwars.logging :as logging]))

(defprotocol Sendable
  (encode [o])
  (decode [o]))

(defn- encode-map [m]
  (with-meta (into {} (for [[k v] m] [(encode k) (encode v)]))
    (encode (meta m))))

(defn- decode-map [m]
  (with-meta (into {} (for [[k v] m] [(decode k) (decode v)]))
    (meta m)))

(extend-protocol Sendable
  aw-map/Coordinate
  (encode [c] (with-meta (list 'coord (:x c) (:y c)) (encode (meta c))))
  (decode [o] o)
  List
  (encode [v] (with-meta (map encode v) (encode (meta v))))
  (decode [o] (with-meta
                (if (and (sequential? o)
                           (= 3 (count o))
                           (= 'coord (first o)))
                  (aw-map/coord (rest o))
                  (map decode o))
                (decode (meta o))))
  EmptyList
  (encode [v] '())
  (decode [o] '())
  HashMap
  (encode [m] (encode-map m))
  (decode [m] (decode-map m))
  ObjMap
  (encode [m] (encode-map m))
  (decode [m] (decode-map m))
  Set
  (encode [s] (with-meta (into #{} (map encode s)) (encode (meta s))))
  (decode [o] (with-meta (into #{} (map decode o)) (decode (meta o))))
  boolean
  (encode [o] o)
  (decode [o] o)
  number
  (encode [o] o)
  (decode [o] o)
  string
  (encode [o] o)
  (decode [o] o)
  nil
  (encode [_] nil)
  (decode [_] nil))

(defn encode-data [data]
  {:pre [(map? data)]}
  (binding [*print-meta* true]
    (pr-str (encode data))))

(defn decode-data [s]
  {:pre [(string? s)]}
  (let [read (reader/read-string s)]
    (assert (map? read))
    (decode-map read)))
