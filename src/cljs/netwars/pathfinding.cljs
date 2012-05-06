(ns netwars.pathfinding
  (:require [netwars.logging :as logging]
            [netwars.aw-map :as aw-map]
            [netwars.path :as aw-path]
            [clojure.set :as set]
            [clojure.string :as string]
            [netwars.logging :as logging]
            [dijkstra :as dijkstra]))

;;; A path is a mutable data structure holding a ordered sequence of coordinates.
;;; Updates using update-path! are mutable.
;;; The data structure can't be used as a normal seq. The sequence of elements is accessable via `elements`

;;; General path functions

(defn make-path [start]
  (atom [start]))

(defn- append! [path c]
  (swap! path conj c))

(defn elements [path]
  @path)

(defn- copy-path! [path source]
  (reset! path (vec (elements source))))

(defn- start [path]
  (first (elements path)))

(defn- end [path]
  (peek (elements path))                ;peek is faster than last on vectors
  )

(defn- length [path]
  (count (elements path)))

(defn in-path? [path c]
  (some #{c} (elements path)))

(declare shortest-path)

;;; TODO: Use netwars.path here
(defn path->aw-path [p]
  (aw-path/make-path (elements p)))

(defn update-path!
  "This function should be called when the mouse touches a new field while pathfinding.
It destructively updates the path according to some magic rules which implement nice pathfinding."
  [path movement-range c terrain-board unit]
  (let [max-length (min (:fuel unit)
                        (:movement-range (meta unit)))]
    (cond
     ;; Element is already in path. Shorten it.
     (in-path? path c)
     (reset! path
             #_(conj (vec (take-while #(not= c %) (elements path))) c)
             (let [els (elements path)]
               (subvec els 0 (inc (count (take-while #(not= c %) els))))))

     ;; Element is right next to the path and not already in path. Add it.
     (and (contains? movement-range c)
          (not (in-path? path c))
          (= 1 (aw-map/distance c (end path)))
          (if max-length (<= (length path) max-length) true))
     (let [shortest (shortest-path (start path) c movement-range terrain-board unit)]
       (if (< (length shortest) (length path))
         (copy-path! path shortest)
         (append! path c)))

     ;; If everything else fails, re-calculate using shortest-path
     (contains? movement-range c)
     (copy-path! path (shortest-path (start path) c movement-range terrain-board unit)))))

(declare dijkstra-wrapper)

(defn shortest-path [start end movement-range terrain-board unit]
  (let [movement-type (:movement-type (meta unit))
        cost-fn (fn [from to]
                  (aw-map/movement-costs (aw-map/at terrain-board to)
                                         movement-type))]
   (dijkstra-wrapper start end movement-range cost-fn)))

;;; Maybe this is more performant without a set
(defn- neighbours [field c]
  (set/intersection (set (map (fn [[x1 y1] c2]
                                (let [;; x1 (:x c1), y1 (:y c1)
                                      x2 (:x c2), y2 (:y c2)]
                                  (aw-map/coord (+ x1 x2) (+ y1 y2))))
                              [[1 0] [-1 0] [0 1] [0 -1]] (repeat c)))
                    field))

;;; TODO: I need to get rid of this conversation to string and back

(let [coords (map aw-map/coord (for [x (range 0 30)
                                     y (range 0 30)] [x y]))

      strings (map #(str (:x %) "," (:y %)) coords)
      cs-table (zipmap coords strings)
      sc-table (zipmap strings coords)]
  (defn- coord->str [c]
    (get cs-table c))

  (defn- str->coord [s]
    (get sc-table s)))

;;; TODO: I should remove this. It's too much for this use case here
(defn- clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings."
  [x]
  (cond
   (string? x) x
   (keyword? x) (name x)
   (aw-map/coord? x) (coord->str x)
   (map? x) (let [a (.-strobj {})]
              (doseq [[k v] x]
                (aset a (clj->js k) (clj->js v)))
              a)
   (coll? x) (apply array (map clj->js x))
   :else x))

;;; TODO: remove the clj->js
(defn- build-graph [movement-range cost-fn]
  ;;; (cost-fn from to) => number
  (clj->js (into {} (for [c movement-range :let [nbs (neighbours movement-range c)]]
                      [c (zipmap nbs (map #(cost-fn c %) nbs))]))) )

(defn- dijkstra-wrapper [start end movement-range cost-fn]
  (logging/log "Searching path from [" (:x start) " " (:y start) "] to [" (:x end) " " (:y end) "]")
  (let [graph (build-graph movement-range cost-fn)
        path (dijkstra/find-path graph (coord->str start) (coord->str end))]
    (atom (map str->coord path))))
