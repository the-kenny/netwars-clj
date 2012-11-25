(ns netwars.net.otw
  (:require netwars.aw-map)             ;import fails w.o. this
  (:use [netwars.aw-map :only [coord]])
  (:import [java.awt Image]
           [java.util List UUID]
           [netwars.aw_map Coordinate]
           [org.apache.commons.codec.binary Base64]))

(defmethod clojure.core/print-method Coordinate [o ^java.io.Writer w]
  (.write w (str "#coord [" (:x o) " " (:y o) "]")))

(defn encode-data [data]
  (binding [*print-meta* true]
    (pr-str data)))

(defn decode-data [s]
  (binding [*read-eval* false]
    (read-string s)))
