(ns netwars.test.map-loader
  (:use clojure.test
        netwars.map-loader
        netwars.aw-map
        [clojure.java.io :only [resource]]))

;;; TODO: Test metadata
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
      (are [x y t] (= t (at (:terrain loaded-map) (coord x y)))
           0 0 :forest
           0 14 :mountain
           8 9 [:port :white]))

    (testing "unit integrity"
      (are [c id color] (let [u (get (:units loaded-map) c)]
                            (and (is (= id (:id u)))
                                 (is (= color (:color u)))))
           (coord 1 8) 0 :red
           (coord 18 14) 21 :black))))

(testing ".aw2 loading"
    (let [map-file "maps/0035.aw2"
          loaded-map (load-map map-file)]
    (is (= 30 (width (:terrain loaded-map))))
    (is (= 20 (height (:terrain loaded-map))))

    (testing "terrain integrity"
      (are [x y t] (= t (at (:terrain loaded-map) (coord x y)))
           0 0 :water
           28 19 :mountain
           1 14 [:headquarter :red]))

    (testing "unit integrity"
      (are [c id color] (let [u (get (:units loaded-map) c)]
                          (and (is (= id (:id u)))
                               (is (= color (:color u)))))
           (coord 1 4) 7 :red
           (coord 18 10) 23 :yellow))))

(testing ".awm loading"
    (let [map-file "maps/1795.awm"
          loaded-map (load-map map-file)]
    (is (= 30 (width (:terrain loaded-map))))
    (is (= 20 (height (:terrain loaded-map))))

    (testing "terrain integrity"
      (are [x y t] (= t (at (:terrain loaded-map) (coord x y)))
           0 0 :water
           5 11 :mountain
           7 13 [:headquarter :blue]))

    (testing "unit integrity"
      (doseq [x (range (width (:terrain loaded-map)))
              y (range (height (:terrain loaded-map)))]
        (is (nil? (get (:units loaded-map) (coord x y)))))))))
