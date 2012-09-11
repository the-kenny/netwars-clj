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
        h #(aw-map/distance % end)
        f (fn [c]
            (+ (get @g c 0)
               (h c)))
        closedset #{}
        ;; It'd be easier to use (into (empty openset) ...) but that
        ;; seems to be broken
        openset (sorted-set-by #(cond
                                 (= %1 %2) 0
                                 (not= %1 %2) (if (= (f %1) (f %2))
                                                (if (< (h %1) (h %2))
                                                  -1 1)
                                                (if (< (f %1) (f %2))
                                                  -1 1))
                                 true 1)
                               start)
        came-from {}]
    (loop [g g
           closedset closedset
           openset openset
           came-from came-from
           limit 50]
      (if-not (pos? limit)
        :loop-limit-exceeded
        (let [x (first openset)
              closedset (conj closedset x)
              edges (remove closedset (neighbours movement-range x))
              ;; _ (println "+++++++++++++++++++++++++++++++++++++++")
              ;; _ (println "x:" x)
              ;; _ (println (str "f(" (:x x) ", "(:y x) "):") (f x))
              ;; _ (println "g:" @g)
              ;; _ (println "openset:" (rest openset))
              ;; _ (println "closedset:" closedset)
              ;; _ (println "edges:" edges)
              [g came-from openset]
              (reduce (fn [[g came-from openset] edge]
                        ;; (println "reduce/openset:" openset)
                        (let [path-costs (+ (aw-map/movement-costs
                                             (board/get-terrain board edge)
                                             movement-type)
                                            (get @g x 0))]
                          (if-not (contains? openset edge)
                            (do
                              ;;   (println "reduce/a:" edge)
                              [(do (swap! g assoc edge path-costs) g)
                               (assoc came-from edge x)
                               (conj openset edge)])
                            (do
                              ;; (println "reduce/b:" edge)
                              [(if (< path-costs (get @g x))
                                 (do (swap! g assoc edge path-costs) g)
                                 g)
                               came-from
                               openset]))))
                      [g came-from (into (sorted-set-by #(cond
                                                          (= %1 %2) 0
                                                          (not= %1 %2) (if (= (f %1) (f %2))
                                                                         (if (< (h %1) (h %2))
                                                                           -1 1)
                                                                         (if (< (f %1) (f %2))
                                                                           -1 1))
                                                          true 1))
                                         (remove #{x} openset))]
                      edges)]
          (if (or (contains? closedset end) (empty? openset))
            (reconstruct-path came-from start end)
            (recur g closedset openset came-from (dec limit))))))))
