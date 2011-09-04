(ns netwars.test.map-utils
  (:use clojure.test
        [clojure.set :as set]
        netwars.map-utils))

(deftest test-neighbours
  (let [terr (netwars.aw-map/make-terrain-board [3 3] [[1 2 3]
                                                       [4 5 6]
                                                       [7 8 9]])]
  (is (= #{:north :east :south :west :north-east :south-east :north-west :south-west}
         (set (keys (neighbours terr (netwars.aw-map/coord 1 1) ))))
      "returns a map with :north :east :south :west and the 4 directions in-between as keys")))

(deftest test-rectangular-direction
  (is (= [:east :west] (rectangular-direction :north)))
  (is (= [:east :west] (rectangular-direction :south)))
  (is (= [:north :south] (rectangular-direction :east)))
  (is (= [:north :south] (rectangular-direction :west)))
  (is (nil? (rectangular-direction :foobar))))
