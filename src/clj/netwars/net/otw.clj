(ns netwars.net.otw
  (:require netwars.aw-map)             ;import fails w.o. this
  (:use [netwars.aw-map :only [coord]])
  (:import [java.awt Image]
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
  (with-meta (map encode o)
    (encode (meta o))))

(defn- decode-seq [o]
  (with-meta (map decode o)
    (decode (meta o))))

(extend-protocol Sendable
  Coordinate
  (encode [c] (list 'coord (:x c) (:y c)))
  (decode [o] o)
  clojure.lang.IRecord
  (encode [v] (encode-map v))
  (decode [v] (decode-map v))
  clojure.lang.ISeq
  (encode [v] (if (and (sequential? v)
                       (= 3 (count v))
                       (= 'coord (first v)))
                (coord (rest v))
                (into (empty v) (encode-seq v))))
  (decode [v] (into (empty v) (decode-seq v)))
  clojure.lang.IPersistentMap
  (encode [m] (encode-map m))
  (decode [m] (decode-map m))
  clojure.lang.IPersistentSet
  (encode [s] (with-meta (into (empty s) (map encode s)) (encode (meta s))))
  (decode [s] (with-meta (into (empty s) (map decode s)) (decode (meta s))))
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
