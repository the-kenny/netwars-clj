(ns netwars.net.otw
  (:require netwars.aw-map)             ;import fails w.o. this
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
  (encode [o]))

(extend-protocol Sendable
  Coordinate
  (encode [c] (with-meta [(:x c) (:y c)]
                {:otw-type :Coordinate}))
  List
  (encode [v] (map encode v))
  IPersistentMap
  (encode [m] (into {} (for [[k v] m] [(encode k) (encode v)])))
  IPersistentSet
  (encode [s] (into #{} (map encode s)))
  UUID
  (encode [u] (str u))
  Object
  (encode [o] o)
  java.awt.Image
  (encode [i] (image-to-base64 i))
  nil
  (encode [_] nil))

(defn encode-data [data]
  {:pre [(map? data)]}
  (binding [*print-meta* true]
    (pr-str (encode data))))

(defn decode-data [s]
  {:pre [(string? s)]}
  (into {} (for [[k v] (read-string s)] [(keyword k) v])))
