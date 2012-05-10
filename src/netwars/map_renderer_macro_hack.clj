(ns netwars.map-renderer-macro-hack
  (:require [netwars.map-utils :as map-utils]))

;*CLJSBUILD-MACRO-FILE*;

(defmacro defconnectable [t1 ts]
  `(defmethod ~'connectable? ~t1 [_# t2#]
     (boolean (#{~@ts} t2#))))

(defmacro def-orientation-method
  "An orientation method should return a list consisting of :u :l :d :r,
  according to the applicable directions where this tile can be connected to."
  [type [neighbours] & body]
  `(defmethod ~'draw-tile ~type [~'_ ~neighbours drawing-fn#]
     (when-let [dirs# (do ~@body)]
       (drawing-fn#
        [~type (~'stringify-directions dirs#)]))))

(defmacro def-straighten-orientation-method [type]
  `(def-orientation-method ~type [nbs#]
     (let [dirseq# (~'get-connectables-directions ~'connectable? ~type nbs#)]
       ;; Streets have at least two endpoints (Special handling for :u and :d)
       (cond
        (or (= dirseq# [:north])
            (= dirseq# [:south])) [:north :south]
        (or (= dirseq# [:east])
            (= dirseq# [:west])
            (empty? dirseq#)) [:east :west]
            true dirseq#))))

(defmacro river-mouth-cond [dir nbs]
  `(and (= :river (~dir ~nbs))
        (every? aw-map/is-water?
                (vals (select-keys ~nbs (map-utils/rectangular-direction ~dir))))))

(defmacro river-mouth-cond [dir nbs]
  `(and (= :river (~dir ~nbs))
        (every? aw-map/is-water?
                (vals  (map-utils/drop-neighbours-behind
                        (~'direction-complement ~dir)
                        ~nbs)))))
