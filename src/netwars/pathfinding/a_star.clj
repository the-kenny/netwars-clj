(ns netwars.pathfinding.a-star
  (:require [netwars.game-board :as board]
            [netwars.aw-map :as aw-map]))

;;; Pathfinding (A*)

(defn ^:private neighbours [movement-range c]
  (let [{:keys [x y]} c]
   (filter movement-range (map (fn [[dx dy]] (aw-map/coord (+ x dx) (+ y dy)))
                               [[0 1] [1 0] [-1 0] [0 -1]]))))

;;; TODO: Handle cyclic paths
(defn ^:private reconstruct-path [came-from start goal]
  (reverse (loop [n goal, path []]
             (if-not (= start n)
               (when-let [n (get came-from n)]
                 (recur n (conj path n)))
               (cons goal path)))))

(defn a-star-path [board movement-range
                   start end]
  (assert (contains? movement-range start))
  (assert (contains? movement-range end))
  (let [movement-type (:movement-type (meta (board/get-unit board start)))
        g (atom {start 0})
        h #(* 2 (aw-map/distance % end))
        f #(+ (get @g % 0) (h %))
        closedset #{}
        openset (sorted-set-by #(compare (f %1) (f %2)) start)
        came-from {}]
    (loop [g g
           closedset closedset
           openset openset
           came-from came-from]
      (let [x (first openset)
            closedset (conj closedset x)
            edges (remove closedset (neighbours movement-range x))

            [g came-from openset]
            (reduce (fn [[g came-from openset] edge]
                      (let [path-costs (+ (aw-map/movement-costs
                                           (board/get-terrain board edge)
                                           movement-type)
                                          (get @g x 0))]
                        (if-not (contains? openset edge)
                          [(do (swap! g assoc edge path-costs) g)
                           (assoc came-from edge x)
                           (conj openset edge)]
                          [(if (< path-costs (get @g x))
                             (do (swap! g assoc path-costs) g)
                             g)
                           came-from
                           openset])))
                    [g came-from (disj openset x)] edges)]
        (if (or (contains? closedset end) (empty? openset))
          (reconstruct-path came-from start end)
          (recur g closedset openset came-from))))))


(comment
  (let [board (:board (netwars.game-creator/make-game {} "7330.aws"))
        start (aw-map/coord 3 14)
        end (aw-map/coord 0 12)
        r (board/reachable-fields board start)]
    (defn test-a-star []
      (= [#netwars.aw_map.Coordinate{:x 3, :y 14}
          #netwars.aw_map.Coordinate{:x 2, :y 14}
          #netwars.aw_map.Coordinate{:x 2, :y 13}
          #netwars.aw_map.Coordinate{:x 1, :y 13}
          #netwars.aw_map.Coordinate{:x 0, :y 13}
          #netwars.aw_map.Coordinate{:x 0, :y 12}]
          (a-star-path board r start end)))

    (defn test-performance-path []
      (print "Path: ")
      (doseq [c (a-star-path board r start end)] (print " ->" (str "(" (:x c) "," (:y c) ")")))
      (prn)
      (time
       (dotimes [_ 1000] (a-star-path board r start end))))))
