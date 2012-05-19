(ns netwars.pathfinding.a-star
  (:require [netwars.game-board :as board]
            [netwars.aw-map :as aw-map]))

;;; Pathfinding (A*)

(defn- neighbours [movement-range c]
  (let [{:keys [x y]} c]
    (filter movement-range (map (fn [[dx dy]] (aw-map/coord (+ x dx) (+ y dy)))
                                [[0 1] [1 0] [-1 0] [0 -1]]))))

;;; TODO: Handle cyclic paths
(defn- reconstruct-path [came-from start goal]
  (reverse (loop [n goal, path []]
             (if-not (= start n)
               (when-let [n (get came-from n)]
                 (recur n (conj path n)))
               (cons goal path)))))

(defn- a-star-path [board movement-range
                    start end]
  (assert (contains? movement-range start))
  (assert (contains? movement-range end))
  (let [movement-type (:movement-type (meta (board/get-unit board start)))
        h (fn [[c _]] (* 2 (aw-map/distance c end)))
        f (fn [[c [costs _]]] (+ costs (* 2 (aw-map/distance c end))))
        closed {}
        open (sorted-set-by #(cond
                              (= %1 %2) 0
                              (not= %1 %2) (if (= (f %1) (f %2))
                                             (if (< (h %1) (h %2))
                                               -1 1)
                                             (if (< (f %1) (f %2))
                                               -1 1))
                              true 1)
                            [start [0 nil]])]
    (reconstruct-path
     (loop [closed closed
            open open
            limit 50]
       (if-let [[c [pcosts p]] (first open)]
         (if-not (= c end)
           (let [closed (assoc closed c p)
                 edges (remove (partial contains? closed) (neighbours movement-range c))
                 open (reduce (fn [open edge]
                                (let [costs (+ pcosts
                                               (aw-map/movement-costs
                                                (board/get-terrain board edge)
                                                movement-type))]
                                  (if-let [oedge (some #(when (= edge (first %)) %) open)]
                                    (let [[c [oldcosts p]] oedge]
                                      (if (< costs oldcosts)
                                        (conj (disj open oedge) [c [costs p]])
                                        open))
                                    (conj open [edge [costs c]]))))
                              (disj open (first open))
                              edges)]
             (recur closed open (dec limit)))
           (assoc closed c p))))
     start end)))


;; (comment
;;   (let [board (:board (netwars.game-creator/make-game {} "7330.aws"))
;;         start (aw-map/coord 3 14)
;;         end (aw-map/coord 0 12)
;;         r (board/reachable-fields board start)]
;;     (defn test-path []
;;       (a-star-path board r start end))
;;     (defn test-a-star []
;;       (= [#netwars.aw_map.Coordinate{:x 3, :y 14}
;;           #netwars.aw_map.Coordinate{:x 2, :y 14}
;;           #netwars.aw_map.Coordinate{:x 2, :y 13}
;;           #netwars.aw_map.Coordinate{:x 1, :y 13}
;;           #netwars.aw_map.Coordinate{:x 0, :y 13}
;;           #netwars.aw_map.Coordinate{:x 0, :y 12}]
;;           (test-path)))

;;     (defn test-performance-path []
;;       (print "Path: ")
;;       (doseq [c (a-star-path board r start end)] (print " ->" (str "(" (:x c) "," (:y c) ")")))
;;       (prn)
;;       (time
;;        (dotimes [_ 1000] (a-star-path board r start end))))))
