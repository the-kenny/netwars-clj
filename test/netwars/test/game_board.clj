(ns netwars.test.game-board
  (:use netwars.game-board
        [netwars.aw-map :only [coord buildings distance]]
        clojure.test
        midje.sweet
        [clojure.set :as set]
        [netwars.map-loader :only [load-map]]
        [netwars.unit-loader :only [load-units]]))

(defn make-testboard []
  (let [loaded-map (load-map "maps/7330.aws")
        unit-spec (load-units "resources/units.xml")
        terrain (:terrain loaded-map)
        units (zipmap (keys (:units loaded-map))
                      (map #(netwars.aw-unit/make-unit unit-spec (:id %) (:color %))
                           (vals (:units loaded-map))))]
    (make-game-board terrain units)))

(deftest test-game-board
  (is (instance? netwars.game_board.GameBoard (make-testboard))))

(deftest test-neutralize-buildings
  (let [color :black]
    (is (empty? (filter (fn [[_ [t c]]] (= c color))
                        (-> (make-testboard)
                            (neutralize-buildings color)
                            :terrain
                            (buildings)
                            ))))))

(deftest test-remove-units
  (let [color :black]
    (is (empty? (filter (fn [[_ u]] (= (:color u) color))
                        (-> (make-testboard)
                            (remove-units color)
                            :units))))))

(def artillery-coord (coord 1 11))
(def infantry-coord (coord 1 13))

(deftest test-attack-functions
  (let [board (make-testboard)]
   (testing "in-attack-range?"
     (is (in-attack-range? board artillery-coord infantry-coord))
     (is (not (in-attack-range? board infantry-coord artillery-coord)))

     ;; Other infantry vs. Artillery (diagonal)
     (is (in-attack-range? board (coord 4 13) (coord 3 14)))
     ;; Non-Range unit vs. unit right next to it
     (is (in-attack-range? board (coord 15 14) (coord 15 13))))))

(deftest test-attack-range
  (let [c (coord 1 8)                   ;Infantry
        range (attack-range (make-testboard) c)]
    (is (set? range))
    (is (= 4 (count range)))
    (is (every? #(= (distance c %) 1) range)))

  ;; Artillery
  (let [range (attack-range (make-testboard) (coord 1 11))]
    (is (set? range))
    (is (= 16 (count range))) ;shoule be 8, but two are outside of the map
    (is (= (set (map coord [[0 13] [1 14] [0 12] [1 13] [2 13] [0 10]
                            [2 12] [0 9] [3 12] [1 9] [2 10] [3 11]
                            [1 8] [2 9] [3 10] [4 11]]))
           range))))


(deftest test-movement-range
  ;; Ugly... but correct
  (let [reachable (reachable-fields (make-testboard) (coord 1 11))]
    (is (set? reachable))
    (is (not (empty? reachable)))
    (is (= reachable (set (map coord
                               [[0 13] [1 14] [2 14] [0 12] [1 12] [2 13]
                                [2 14] [0 11] [2 12] [1 11] [0 10] [3 13]
                                [3 12] [4 13] [0 9]  [4 12] [3 11] [0 8] [1 9]])))))

  (let [reachable (reachable-fields (make-testboard) (coord 4 13))]
    (is (set? reachable))
    (is (not (empty? reachable)))
    (is (= reachable (set (map coord
                               [[1 14] [0 12] [1 12] [2 14] [5 13]
                                [2 13] [1 11] [2 12] [3 13] [4 14]
                                [3 12] [4 13] [3 11] [4 12]])))))


  (let [reachable (reachable-fields (make-testboard) (coord 13 2))]
    (is (set? reachable))
    (is (not (empty? reachable)))
    (is (= reachable (set (map coord
                               [[18 1] [17 0] [8 3] [9 4] [10 5]
                                [11 6] [12 7] [9 3] [10 4] [11 5]
                                [12 6] [13 7] [10 3] [12 5] [11 4]
                                [13 6] [10 2] [11 3] [12 4] [13 5]
                                [14 6] [9 1] [11 2] [12 3] [13 4]
                                [14 5] [10 1] [15 6] [12 2] [13 3]
                                [14 4] [11 1] [15 5] [10 0] [13 2]
                                [14 3] [12 1] [15 4] [11 0] [14 2]
                                [13 1] [15 3] [12 0] [14 1] [13 0]
                                [15 1] [14 0] [15 0] [16 5] [16 4]
                                [16 3] [17 4] [17 3] [16 2] [16 1]
                                [18 3] [17 2] [17 1] [16 0]]))))))

(defn test-movement-range-performance []
  (let [board (make-testboard)]
    (time
     (dotimes [_ 100]
       (reachable-fields (make-testboard) (coord 1 11))))))
;; (test-movement-range-performance) => 1630.004 msecs

(defn time-test-movement-range []
  (test-movement-range)
  (test-movement-range-performance))
