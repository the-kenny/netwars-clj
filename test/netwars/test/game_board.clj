(ns netwars.test.game-board
  (:use netwars.game-board
        [netwars.aw-map :only [coord]]
        clojure.test
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

(deftest test-movement-range
  ;; Ugly... but correct
  (let [reachable (reachable-fields (make-testboard) (coord 1 11))]
    (is (set? reachable))
    (is (not (empty? reachable)))
    (is (= reachable (set (map #(apply coord %)
                               [[0 13] [1 14] [2 14] [0 12] [1 12] [2 13]
                                [2 14] [0 11] [2 12] [1 11] [0 10] [3 13]
                                [3 12] [4 13] [0 9]  [4 12] [3 11] [0 8] [1 9]])))))
  (let [reachable (reachable-fields (make-testboard) (coord 4 13))]
    (is (set? reachable))
    (is (not (empty? reachable)))
    (is (= reachable
           (set (map #(apply coord %)
                     [[1 14] [0 12] [1 12] [2 14] [5 13]
                      [2 13] [1 11] [2 12] [3 13] [4 14]
                      [3 12] [4 13] [3 11] [4 12]
                      ]))))))
