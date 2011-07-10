(ns netwars.map-loader-tests
  (:use clojure.test
        netwars.map-loader
        netwars.aw-map
        [clojure.java.io :only [resource]]))

(deftest map-loading
  (let [map-file "maps/7330.aws"]
    (is (instance? netwars.map_loader.LoadedMap (load-map map-file))
        "loads a map from map-file"))
  (let [url "http://www.advancewarsnet.com/designmaps/mapfiles/7330.aws"]
    (is (instance? netwars.map_loader.LoadedMap (load-map url))
        "loads a map from an url"))

  (testing ".aws loading"
    (let [map-file "maps/7330.aws"
          loaded-map (load-map map-file)]
    (is (= 20 (width (:terrain loaded-map))))
    (is (= 15 (height (:terrain loaded-map))))

    (testing "terrain integrity"
      (let [terrain (:terrain loaded-map)]
       (is (= :forest (at terrain (coord 0 0))))
       (is (= :mountain (at terrain (coord 0 14))))
       (is (= [:port :white] (at terrain (coord 8 9))))))

    (testing "unit integrity"
      (is (let [unit (get (:units loaded-map) (coord 1 8))]
          (and (= 0 (:id unit))
               (= :red (:color unit)))))
      (is (let [unit (get (:units loaded-map) (coord 18 14))]
          (and (= 21 (:id unit))
               (= :black (:color unit))))))))

(testing ".aw2 loading"
    (let [map-file "maps/0035.aw2"
          loaded-map (load-map map-file)]
    (is (= 30 (width (:terrain loaded-map))))
    (is (= 20 (height (:terrain loaded-map))))

    (testing "terrain integrity"
      (let [terrain (:terrain loaded-map)]
       (is (= :water (at terrain (coord 0 0))))
       (is (= :mountain (at terrain (coord 28 19))))
       (is (= [:headquarter :red] (at terrain (coord 1 14))))))

    (testing "unit integrity"
      (is (let [unit (get (:units loaded-map) (coord 1 4))]
          (and (= 7 (:id unit))
               (= :red (:color unit)))))
      (is (let [unit (get (:units loaded-map) (coord 18 10))]
          (and (= 23 (:id unit))
               (= :yellow (:color unit))))))))

(testing ".awm loading"
    (let [map-file "maps/1795.awm"
          loaded-map (load-map map-file)]
    (is (= 30 (width (:terrain loaded-map))))
    (is (= 20 (height (:terrain loaded-map))))

    (testing "terrain integrity"
      (let [terrain (:terrain loaded-map)]
       (is (= :water (at terrain (coord 0 0))))
       (is (= :mountain (at terrain (coord 5 11))))
       (is (= [:headquarter :blue] (at terrain (coord 7 13))))))

    (testing "unit integrity"
      (doseq [x (range (width (:terrain loaded-map)))
              y (range (height (:terrain loaded-map)))]
        (is (nil? (get (:units loaded-map) (coord x y)))))))))
