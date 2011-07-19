(ns netwars.test.game-board
  (:use netwars.game-board
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
