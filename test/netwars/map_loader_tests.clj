(ns netwars.map-loader-tests
  (:use [lazytest.describe :only [describe do-it given testing it with]]
        [lazytest.expect :only [expect]]
        netwars.map-loader
        netwars.aw-map)
  (:import [netwars.aw-map AwMap]))

(describe load-map
  (given [map-file "/Users/moritz/Development/Clojure/netwars/maps/7330.aws"]
    (it "loads a map from map-file"
      (instance? AwMap (load-map map-file)))))

(describe ".aws map loading" 
  (given [map-file "/Users/moritz/Development/Clojure/netwars/maps/7330.aws"
          loaded-map (load-map map-file)]
    (it "has a width of 20"
      (= 20 (width loaded-map)))
    (it "has a height of 15"
      (= 15 (height loaded-map)))
    (it "has width columns in :terrain"
      (= (count (:terrain loaded-map)) (width loaded-map)))
    (it "has height items in every row"
      (every? #(= (height loaded-map) (count %)) (:terrain loaded-map)))

    (testing element-at
      (it "returns a vector of the terrain, and nil or the unit"
        (= [:forest nil] (element-at loaded-map (coord 0 0)))))

    (testing terrain-at
      (it "returns :forest at [0 0]"
        (= :forest (terrain-at loaded-map (coord 0 0))))
      (it "returns :mountain at [0 19]"
        (= :mountain (terrain-at loaded-map (coord 0 14))))
      (it "returns [:port :white] at [8 9]"
        (= [:port :white] (terrain-at loaded-map (coord 8 9)))))

    (testing unit-at
      (it "returns an AwMapUnit with id 0 and color :red at [1 8]"
        (let [unit (unit-at loaded-map (coord 1 8))]
          (and (= 0 (:id unit))
               (= :red (:color unit)))))
      (it "returns an AwMapUnit with id 21 and color :black at [18 14]"
        (let [unit (unit-at loaded-map (coord 18 14))]
          (and (= 21 (:id unit))
               (= :black (:color unit))))))))

(describe ".aw2 map loading" 
  (given [map-file "/Users/moritz/Development/Clojure/netwars/maps/0035.aw2"
          loaded-map (load-map map-file)]
    (it "has a width of 30"
      (= 30 (width loaded-map)))
    (it "has a height of 20"
      (= 20 (height loaded-map)))
    (it "has width columns in :terrain"
      (= (count (:terrain loaded-map)) (width loaded-map)))
    (it "has height items in every row"
      (every? #(= (height loaded-map) (count %)) (:terrain loaded-map)))

    (testing terrain-at
      (it "returns :water at [0 0]"
        (= :water (terrain-at loaded-map (coord 0 0))))
      (it "returns :mountain at [28 19]"
        (= :mountain (terrain-at loaded-map (coord 28 19))))
      (it "returns [:headquarter :red] at [1 14]"
        (= [:headquarter :red] (terrain-at loaded-map (coord 1 14)))))
    
    (testing unit-at
      (it "returns an AwMapUnit with id 7 (Battleship) and color :red at [1 4]"
        (let [unit (unit-at loaded-map (coord 1 4))]
          (and (= 7 (:id unit))
               (= :red (:color unit)))))
      (it "returns an AwMapUnit with id 23 (Rockets) and color :yellow at [18 10]"
        (let [unit (unit-at loaded-map (coord 18 10))]
          (and (= 23 (:id unit))
               (= :yellow (:color unit))))))))

(describe ".awm map loading" 
  (given [map-file "/Users/moritz/Development/Clojure/netwars/maps/1795.awm"
          loaded-map (load-map map-file)]
    (it "has a width of 30"
      (= 30 (width loaded-map)))
    (it "has a height of 20"
      (= 20 (height loaded-map)))
    (it "has width columns in :terrain"
      (= (count (:terrain loaded-map)) (width loaded-map)))
    (it "has height items in every row"
      (every? #(= (height loaded-map) (count %)) (:terrain loaded-map)))

    (testing terrain-at
      (it "returns :water at [0 0]"
        (= :water (terrain-at loaded-map (coord 0 0))))
      (it "returns :mountain at [5 11]"
        (= :mountain (terrain-at loaded-map (coord 5 11))))
      (it "returns [:headquarter :blue] at [7 13]"
        (= [:headquarter :blue] (terrain-at loaded-map (coord 7 13)))))
    
    (testing unit-at
      (do-it "returns nil for all coordinates"
        (doseq [x (range (width loaded-map)) y (range (height loaded-map))]
          (expect (nil? (unit-at loaded-map (coord x y)))))))))
