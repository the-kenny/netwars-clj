(ns netwars.aw-map-tests
  (:use netwars.aw-map
        lazytest.describe)
  (:import [netwars.aw-map Coordinate AwMapUnit AwMap]))


;;; Given map:
;;;  -------
;;; | w p w |
;;; | w w w |
;;;  -------
(describe AwMap
  (given [m (AwMap. nil [3 2] [[:water :water]
                               [:plain :water]
                               [:water :water]]
                    {(Coordinate. 1 0) (AwMapUnit. 42 :red)})]
    (it "has a width of 3"
      (= (width m) 3))
    (it "has a height of 2"
      (= (height m) 2))

    (testing in-bounds?
      (it "returns false for coordinates outside of the map"
        (every? #(not (in-bounds? m %)) [(Coordinate. -1 0)
                                        (Coordinate. 3 0)
                                        (Coordinate. 0 2)]))
      (it "returns true for coordinates inside the bounds of the map"
        (every? #(in-bounds? m %) [(Coordinate. 0 0)
                                  (Coordinate. 1 1)
                                  (Coordinate. 2 1)])))
    
    (testing unit-at
      (it "returns an AwMapUnit for [1 0]"
        (instance? AwMapUnit (unit-at m (Coordinate. 1 0))))
      (it "returns nil for every other coordinate"
        (every? nil? (for [x (range (width m))
                           y (range ( height m)) :while (and (not= x 1)
                                                            (not= y 0))]
                       (unit-at m (Coordinate. x y))))))
    (testing terrain-at
      (it "returns :plain at [1 0]"
        (= :plain (terrain-at m (Coordinate. 1 0)))))))
