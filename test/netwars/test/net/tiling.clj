(ns netwars.test.net.tiling
  (:use clojure.test
        [netwars.net.tiling :as tiling]
        [netwars.aw-map :only [coord?]]))

(deftest test-load-tile
  (is (vector? (load-tile "resources/pixmaps/units/")) "returns a vector")
  (is (= 2 (count (load-tile "resources/pixmaps/units/"))) "...of length 2")
  (let [[spec tile] (load-tile "resources/pixmaps/units/")]
    (testing "tiling-spec"
      (is (map? spec) "spec is a map")
      (doseq [[p c] spec]
        (is (vector? p) "key is a vector")
        (doseq [pe p] (is (keyword? pe) "...of keywords"))
        (is (coord? c))))
    (testing "tile"
      (is (instance? java.awt.Image tile))
      (doseq [[_ c] spec]
       (is (and (< (:x c) (.getWidth tile))
                (< (:y c) (.getHeight tile)))
           "Every spec-coord is within tile's bounds")))))
