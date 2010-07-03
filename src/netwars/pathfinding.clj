(ns netwars.pathfinding
  (:use [clojure.contrib.graph :as graph]
        [netwars.map-loader :as map-loader]
        [netwars.map-utils :as map]))

(defn- make-neighbor [awmap x y]
  (when (on-map awmap x y)
    [x y (terrain-at awmap x y)]))

(defn- make-traversable-node-seq [awmap unit]
  (for [x (range (:width awmap))
        y (range (:height awmap))
        :when (map/can-pass (:movement-type unit) (terrain-at awmap x y))]
    (make-neighbor awmap x y)))

(defn- traversable-node-connections [awmap node unit]
  (let [[x y _] node]
    (filter (fn [[_ _ terr]]
              (map/can-pass? (:movement-type unit) terr))
            (remove nil?
                    [(make-neighbor awmap (dec x) y)
                     (make-neighbor awmap (inc x) y)
                     (make-neighbor awmap x (inc y))
                     (make-neighbor awmap x (dec y))]))))

(defn- create-weighted-graph [awmap unit]
  (let [nodes (make-traversable-node-seq awmap unit)
        neighbors #(traversable-node-connections awmap % unit)
        weights (fn [[_ _ terrain]]
                  (map/movement-costs terrain (:movement-type unit)))]
   (struct-map graph/directed-graph
     :nodes nodes
     :neighbors neighbors
     :weights weights)))

