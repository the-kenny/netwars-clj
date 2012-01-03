(ns netwars.net.otw
  (:require [netwars.aw-map :as aw-map]
            [cljs.reader :as reader]
            [netwars.logging :as logging]))

(defprotocol Sendable
  (encode [o]))

(defn- encode-map [m]
  (with-meta (into {} (for [[k v] m] [(encode k) (encode v)]))
    (encode (meta m))))

(extend-protocol Sendable
  aw-map/Coordinate
  (encode [c] (with-meta (list 'coord (:x c) (:y c)) (encode (meta c))))
  List
  (encode [v] (with-meta (map encode v) (encode (meta v))))
  HashMap
  (encode [m] (encode-map m))
  ObjMap
  (encode [m] (encode-map m))
  Set
  (encode [s] (with-meta (into #{} (map encode s)) (encode (meta s))))
  boolean
  (encode [o] o)
  number
  (encode [o] o)
  string
  (encode [o] o)
  nil
  (encode [_] nil))

(defn encode-data [data]
  {:pre [(map? data)]}
  (binding [*print-meta* true]
    (pr-str (encode data))))

(defmulti ^{:private true} decode #(sequential? %))

(defmethod decode :default [o]
  o)

(defmethod decode true [l]
  (logging/log (str l))
  (let [[f & r] l]
    (if (and (= f 'coord)
             (= 2 (count r)))
      (aw-map/coord r)
      l)))

(defn decode-data [s]
  {:pre [(string? s)]}
  (let [read (reader/read-string s)]
   (with-meta (into {} (for [[k v] read] [(keyword k) (decode v)]))
     (meta read))))
