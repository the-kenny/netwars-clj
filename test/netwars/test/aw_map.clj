(ns netwars.test.aw-map
  (:use netwars.aw-map
        clojure.test))

(deftest Coordinate
  (let [c (coord 2 3)]
    (is (= (:x c) 2) "has an x component of 2")
    (is (= (:y c) 3) "has an y component of 3")))


;;; Given map:
;;;  -------
;;; | w p w |
;;; | w w w |
;;;  -------
(deftest TerrainBoard
  (let [m (make-terrain-board [3 2] [[:water :water]
                                       [:plain :water]
                                       [:water :water]])]
    (is (= (width m) 3) "has a width of 3")
    (is (= (height m) 2) "has a height of 2")

    (testing in-bounds?
      (is (every? #(not (in-bounds? m %)) [(coord -1 0)
                                           (coord 3 0)
                                           (coord 0 2)])
          "returns false for coordinates outside of the map")
      (is (every? #(in-bounds? m %) [(coord 0 0)
                                     (coord 1 1)
                                     (coord 2 1)])
          "returns true for coordinates inside the bounds of the map"))))
