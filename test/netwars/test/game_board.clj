(ns netwars.test.game-board
  (:use netwars.game-board
        [netwars.aw-map :only [coord]]
        clojure.test)
  (:require netwars.map-loader
            netwars.unit-loader))

(defn make-testboard []
  (let [loaded-map (netwars.map-loader/load-map "maps/7330.aws")
        unit-spec (netwars.unit-loader/load-units "resources/units.xml")
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
