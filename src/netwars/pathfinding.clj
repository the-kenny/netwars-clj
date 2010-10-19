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
        :when (map/can-pass? (:movement-type unit) (terrain-at awmap x y))]
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

(defn weight [graph node]
  ((:weights graph) node))

;;; Pathfinding (A*)

(defn- manhattan-distance [[x1 y1 _] [x2 y2 _]]
  (+ (Math/abs (- x1 y1)) (Math/abs (- x2 y2))))

(comment
  (defn- euclidean-distance [[x1 y1 _] [x2 y2 _]]
    (letfn [(square [n] (* n n))]
      (Math/sqrt (+ (square (- x1 x2)) (square (- y1 y2)))))))

(defn dist [a b] (manhattan-distance a b))

(defn- reconstruct-path [came-from start goal]
  (println "Reconstructing path...")
  (reverse (loop [n goal, path []]
             (if-not (= start n)
               (when-let [n (get came-from n)]
                 (recur n (conj path n)))
               (cons goal path)))))

(defn- a-star-path [graph start end] 
  (let [g (ref {start 0})
        h #(dist % end)
        f #(+ (get @g % 0) (h %))
        closedset (ref #{})
        openset (ref (sorted-set-by #(compare (f %1) (f %2)) start))
        came-from (ref {})]
    (loop []
      (let [x (first @openset)]
        (dosync
         (alter closedset conj x)
         (alter openset disj x)
         (doseq [nb (remove @closedset (graph/get-neighbors graph x))]
           (if-not (@openset nb)
             (do
               (alter g assoc nb (+ (weight graph nb) (get @g x 0)))
               ;; (alter h assoc nb (dist nb end))
               (alter came-from assoc nb x)
               (alter openset conj nb))
             (when (< (+ (weight graph nb) (get @g x 0)) (get @g nb 0))
               (alter g assoc nb (+ (weight graph nb) (@g x)))
               ;; (alter h assoc nb (dist nb end))
               ))))
        (if-not (or (@closedset end)
                    (empty? @openset))
          (recur)
          (reconstruct-path @came-from start end))))))

(comment
  (def +lmap+ (map-loader/load-map
               "http://advancewarsnet.com/designmaps/mapfiles/0777.aw2"))
  (def +g+ (create-weighted-graph +lmap+  {:movement-type :tires}))

  (def start-node (make-neighbor +lmap+ 0 0))
  (def goal-node (make-neighbor +lmap+ 0 3))

  (a-star-path +g+ start-node goal-node))
