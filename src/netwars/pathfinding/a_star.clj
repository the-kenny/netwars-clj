(ns netwars.pathfinding.a-star
  (:require [netwars.game-board :as board]
            [netwars.aw-map :as aw-map]))

;;; Pathfinding (A*)

(defn- neighbours [movement-range c]
  (let [{:keys [x y]} c]
   (filter movement-range (map (fn [[dx dy]] (aw-map/coord (+ x dx) (+ y dy)))
                               [[0 1] [1 0] [-1 0] [0 -1]]))))

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

        g (atom {start 0})
        h #(* 2 (aw-map/distance % end))
        f #(+ (get @g % 0) (h %))
        closedset (atom #{})
        openset (atom (sorted-set-by #(compare (f %1) (f %2)) start))
        came-from (atom {})]
    (loop []
      (let [x (first @openset)]
        (swap! closedset conj x)
        (swap! openset disj x)
        (doseq [nb (remove @closedset (neighbours movement-range x))
                :let [terrain (board/get-terrain board nb)]]
          (if-not (@openset nb)
            (do
              (swap! g assoc nb (+ (aw-map/movement-costs terrain movement-type)
                                   (get @g x 0)))
              (swap! came-from assoc nb x)
              (swap! openset conj nb))
            (when (< (+ (aw-map/movement-costs terrain movement-type)
                        (get @g x 0))
                     (get @g nb 0))
              (swap! g assoc nb (+ (aw-map/movement-costs terrain movement-type)
                                   (@g x))))))
        (if-not (or (@closedset end)
                    (empty? @openset))
          (recur)
          (reconstruct-path @came-from start end))))))

(comment
  (let [board (:board (netwars.game-creator/make-game {} "7330.aws"))
        start (coord 3 14)
        end (coord 0 12)
        r (board/reachable-fields board start)]
    (doseq [c  (a-star-path board r start end)] (println (:x c) (:y c)))))
