(ns netwars.aw-unit-tests
  (:use netwars.aw-unit
        [clojure.java.io :only [resource]]
        [netwars.unit-loader :as loader]
        clojure.test))

(deftest unit-preparation
  (let [spec (loader/load-units (resource "units.xml"))
        weapon-unit (make-unit spec 0 :red)]  ;Infantry
    (is (instance? netwars.aw_unit.AwUnit weapon-unit))
    (is (= (:internal-name weapon-unit) :infantry))
    (is (= (:color weapon-unit :red)))
    (is (= (:hp weapon-unit 10)))
    (is (= (:fuel weapon-unit 99)))

    (testing "weapon properties"
      (doseq [w (:weapons weapon-unit)]
        (is (every? #{:name :ammo :range :distance} (keys w)))
        (is (instance? String (:name w)))
        (is (every? integer? (vals (dissoc w :name))))))))
