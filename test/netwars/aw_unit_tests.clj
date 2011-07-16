(ns netwars.aw-unit-tests
  (:use netwars.aw-unit
        [clojure.java.io :only [resource]]
        [netwars.unit-loader :as loader]
        clojure.test))

(deftest unit-preparation
  (let [spec (loader/load-units (resource "units.xml"))
        weapon-unit (make-unit spec 0 :red)  ;Infantry
        transport-unit (make-unit spec 22 :green)]
    (is (instance? netwars.aw_unit.AwUnit weapon-unit))
    (is (= (:internal-name weapon-unit) :infantry))
    (is (= (:color weapon-unit :red)))
    (is (= (:hp weapon-unit 10)))
    (is (= (:fuel weapon-unit 99)))

    (testing "weapon properties"
      (is (contains? weapon-unit :weapons))
      (is (> (count (:weapons weapon-unit)) 0))
      (doseq [w (:weapons weapon-unit)]
        (is (every? #{:name :ammo :range :distance} (keys w)))
        (is (instance? String (:name w)))
        (is (every? integer? (vals (dissoc w :name))))))

    (testing "freight"
      (is (contains? transport-unit :transport))
      (let [transport (:transport transport-unit)]
        (is (map? transport))
        (is (> (:limit transport) 0))
        (is (= [] (:freight transport)))))))
