(ns netwars.impl.map-loader
  (:require [clojure.java.io :as io])
  (:import java.nio.ByteBuffer))

(defmacro read-when-possible [buf & body]
  `(when (.hasRemaining ~buf)
     ~@body))

(defn read-binary-resource [source]
  (with-open [bos (java.io.ByteArrayOutputStream.)
              is (io/input-stream source)]
    (io/copy is bos)
    (java.nio.ByteBuffer/wrap (.toByteArray bos))))

(defn read-n-string [buf len]
  (if (> len 0)
   (let [arr (make-array Byte/TYPE len)]
     (.get buf arr)
     (apply str (map char  arr)))
   ""))

(defn read-null-string [buf]
  (loop [s ""]
	(if-let [c #(let [c (.getChar buf)] (if (= % 0) nil %))]
	  (recur (str s c))
	  s)))

(defn read-byte [#^ByteBuffer buf]
  (.get buf))

(defn read-dword [#^ByteBuffer buf]
  (.getShort buf))

(defn read-int32 [#^ByteBuffer buf]
  (.getInt buf))
