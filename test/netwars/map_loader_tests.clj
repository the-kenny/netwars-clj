(ns netwars.map-loader-tests
  (:use [lazytest.describe :only [describe given testing it with]]
        netwars.map-loader
        [netwars.unit-loader :only [load-units!]]
        [netwars.utilities :only [load-resource]]))

(describe load-map
  (given [map-file "/Users/moritz/Development/Clojure/netwars/bla.aws"]
    (it "loads a map from map-file"
      (load-map map-file))))

;;; Hack: Setup environment
(load-units! (load-resource "units.xml"))

(describe "A loaded map" 
  (given [map-file "/Users/moritz/Development/Clojure/netwars/bla.aws"
          loaded-map (load-map map-file)]
    (it "has the same filename as `map-file`"
      (= (:filename loaded-map) map-file))
    (it "has a width > 0"
      (> (:width loaded-map) 0))
    (it "has a height > 0"
      (> (:width loaded-map) 0))
    (it "has (* width height) fields in :terrain-data"
      (= (count (:terrain-data loaded-map)) (* (:width loaded-map)
                                               (:height loaded-map))))
    (it "has a tileset from: [:normal :snow :desert :wasteland :aw1 :aw2]"
      (some #{(:tileset loaded-map)}
            (set [:normal :snow :desert :wasteland :aw1 :aw2])))

    (testing "(:unit-data map)"      
      (it "returns a sequence for of units otherwise"
        (coll? (:unit-data loaded-map)))
        
      (testing "Every unit in this collection"
        (it "has the form of [[int int] [id color]]"
          (every? (fn [[[x y] [id color]]]
                    (and (integer? x)
                         (integer? y)
                         (>= id 0)
                         (#{:red :blue :green :yellow :black} color)))
                  (:unit-data loaded-map)))
        (it "is located in the map's bounds"
          (every? (fn [[[x y] _]]
                    (and (< -1 x (:width loaded-map))
                         (< -1 y (:height loaded-map))))
                  (:unit-data loaded-map)))))))
