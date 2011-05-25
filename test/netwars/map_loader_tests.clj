(ns netwars.map-loader-tests
  (:use [lazytest.describe :only [describe given testing it with]]
        netwars.map-loader
        netwars.aw-map)
  (:import [netwars.aw-map Coordinate AwMap]))

(describe load-map
  (given [map-file "/Users/moritz/Development/Clojure/netwars/7330.aws"]
    (it "loads a map from map-file"
      (instance? AwMap (load-map map-file)))))

(describe "A loaded map" 
  (given [map-file "/Users/moritz/Development/Clojure/netwars/7330.aws"
          loaded-map (load-map map-file)]
    (it "has a width > 0"
      (pos? (width loaded-map)))
    (it "has a height > 0"
      (pos? (height loaded-map)))
    (it "has width columns in :terrain"
      (= (count (:terrain loaded-map)) (width loaded-map)))
    (it "has height items in every row"
      (every? #(= (height loaded-map) (count %)) (:terrain loaded-map)))

    (testing element-at
      (it "returns a vector of the terrain, and nil or the unit"
        (= [:forest nil] (element-at loaded-map (Coordinate. 0 0)))))

    (testing terrain-at
      (it "returns :forest at [0 0]"
        (= :forest (terrain-at loaded-map (Coordinate. 0 0))))
      (it "returns :mountain at [0 19]"
        (= :mountain (terrain-at loaded-map (Coordinate. 0 14))))
      (it "returns [:port :white] at [8 9]"
        (= [:port :white] (terrain-at loaded-map (Coordinate. 8 9)))))

    (testing unit-at
      (it "returns an AwMapUnit with id 0 and color :red at [1 8]"
        (let [unit (unit-at loaded-map (Coordinate. 1 8))]
          (and (= 0 (:id unit))
               (= :red (:color unit)))))
      (it "returns an AwMapUnit with id 21 and color :black at [18 14]"
        (let [unit (unit-at loaded-map (Coordinate. 18 14))]
          (and (= 21 (:id unit))
               (= :black (:color unit))))))))
