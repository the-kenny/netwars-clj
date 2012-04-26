(ns netwars.net.otw
  (:require [netwars.aw-map :as aw-map]
            [cljs.reader :as reader]
            [netwars.logging :as logging]
            [cljs.core :as cljs.core]))

(defprotocol Sendable
  (encode [o])
  (decode [o]))

(defn- encode-map [m]
  (with-meta (into {} (for [[k v] m] [(encode k) (encode v)]))
    (encode (meta m))))

(defn- decode-map [m]
  (with-meta (into {} (for [[k v] m] [(decode k) (decode v)]))
    (meta m)))

(defn- encode-seq [v]
  (with-meta (map encode v) (encode (meta v))))

(defn- decode-seq [o]
  (with-meta
    (if (and (= 3 (count o))
             (= 'coord (first o)))
      (aw-map/coord (rest o))
      (into (empty o) (map decode o)))
    (decode (meta o))))

(extend-protocol Sendable
  List
  (encode [s] (encode-seq s))
  (decode [s] (decode-seq s))
  PersistentVector
  (encode [s] (encode-seq s))
  (decode [s] (decode-seq s))
  EmptyList
  (encode [s] (encode-seq s))
  (decode [s] (decode-seq s)))

(extend-protocol Sendable
  ;; PersistentHashMap
  ;; (encode [m] (encode-map m))
  ;; (decode [m] (decode-map m))
  ;; PersistentTreeMap
  ;; (encode [m] (encode-map m))
  ;; (decode [m] (decode-map m))
  HashMap
  (encode [m] (encode-map m))
  (decode [m] (decode-map m))
  ObjMap
  (encode [m] (encode-map m))
  (decode [m] (decode-map m)))

(extend-protocol Sendable
  Set
  (encode [s] (with-meta (into #{} (map encode s)) (encode (meta s))))
  (decode [o] (with-meta (into #{} (map decode o)) (decode (meta o)))))

(extend-protocol Sendable
  aw-map/Coordinate
  (encode [c] (with-meta (list 'coord (:x c) (:y c)) (encode (meta c))))
  (decode [o] o))

(extend-protocol Sendable
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
