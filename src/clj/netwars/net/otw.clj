(ns netwars.net.otw
  (:require netwars.aw-map)             ;import fails w.o. this
  (:use [netwars.aw-map :only [coord]])
  (:import [clojure.lang IPersistentMap IPersistentSet Keyword]
           [java.awt Image]
           [java.util List UUID]
           [netwars.aw_map Coordinate]
           [org.apache.commons.codec.binary Base64]))

(defn image-to-base64 [image]
  (let [os (java.io.ByteArrayOutputStream.)
        output (java.io.StringWriter.)]
    (javax.imageio.ImageIO/write image "png" os)
    (str "data:image/png;base64,"
         (Base64/encodeBase64String (.toByteArray os)))))

(defprotocol Sendable
  (encode [o])
  (decode [o]))

(declare encode decode)                 ;Don't know why this is needed

(defn- encode-map [m]
  (with-meta (into {} (for [[k v] m] [(encode k) (encode v)]))
    (encode (meta m))))

(defn- decode-map [m]
  (with-meta (into {} (for [[k v] m] [(decode k) (decode v)]))
    (decode (meta m))))

(defn- encode-seq [o]
  (with-meta (map encode o) (encode (meta o))))

(defn- decode-seq [o]
  (with-meta
    (if (and (sequential? o)
             (= 3 (count o))
             (= 'coord (first o)))
      (coord (rest o))
      (map decode o))
    (decode (meta o))))

(extend-protocol Sendable
  Coordinate
  (encode [c] (with-meta (list 'coord (:x c) (:y c)) (encode (meta c))))
  (decode [o] o)
  List
  (encode [v] (encode-seq v))
  (decode [v] (decode-seq v))
  IPersistentMap
  (encode [m] (encode-map m))
  (decode [m] (decode-map m))
  IPersistentSet
  (encode [s] (with-meta (into #{} (map encode s)) (encode (meta s))))
  (decode [s] (with-meta (into #{} (map decode s)) (decode (meta s))))
  UUID
  (encode [u] (str u))
  (decode [u] (assert nil "unimplemented"))
  Object
  (encode [o] o)
  (decode [o] o)
  java.awt.Image
  (encode [i] (image-to-base64 i))
  (decode [o] (assert nil "unimplemented")) ;TODO: image-from-base64
  nil
  (encode [_] nil)
  (decode [_] nil))

(defn encode-data [data]
  {:pre [(map? data)]}
  (binding [*print-meta* true]
    (pr-str (encode data))))

(defn decode-data [s]
  {:pre [(string? s)]}
  (let [read (read-string s)]
    (decode-map read)))
