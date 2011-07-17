(ns netwars.aw-unit-tests
  (:use netwars.aw-unit
        [clojure.java.io :only [resource]]
        [netwars.unit-loader :as loader]
        clojure.test))

(deftest test-unit-creation
  (let [spec (loader/load-units (resource "units.xml"))
        weapon-unit (make-unit spec 0 :red)  ;Infantry
        transport-unit (make-unit spec 22 :green)]
    (is (instance? netwars.aw_unit.AwUnit weapon-unit))
    (is (= (:internal-name weapon-unit) :infantry))
    (is (= (:color weapon-unit :red)))
    (is (= (:hp weapon-unit 10)))
    (is (= (:fuel weapon-unit 99)))

    (is (= (meta weapon-unit)
           (loader/find-prototype spec :internal-name (:internal-name weapon-unit)))
        "Instance contains prototype as meta")

    (testing "weapon properties"
      (is (contains? weapon-unit :weapons))
      (is (> (count (:weapons weapon-unit)) 0))
      (doseq [w (:weapons weapon-unit)]
        (is (every? #{:name :ammo :range :distance} (keys w)))
        (is (instance? String (:name w)))
        (is (or (integer? (:ammo w)) (= :infinity (:ammo w))))
        (is (every? integer? (vals (dissoc w :name :ammo))))
        (is (meta w) "Has the weapon metadata (the spec) attached?")))

    (testing "freight"
      (is (contains? transport-unit :transport))
      (let [transport (:transport transport-unit)]
        (is (map? transport))
        (is (> (:limit transport) 0))
        (is (= [] (:freight transport)))))))

(deftest test-transporting
  (let [spec (loader/load-units (resource "units.xml"))
        transporter (make-unit spec 22 :green)
        unit (make-unit spec 0 :red)  ;Infantry
        ]
    (is (can-transport? transporter))
    (is (= #{:infantry :mech} (transport-types transporter)))

    (testing "loading"
      (is (contains? (transport-types transporter) (:internal-name unit)))
      (let [loaded (transport-unit transporter unit)]
        (is (= [] (-> transporter :transport :freight)))
        (is (= unit (get-in loaded [:transport :freight 0])))))

    (testing "unloading"
      (let [loaded (transport-unit transporter unit)]
        (is (= [transporter unit] (unload-unit loaded 0)))
        (is (thrown? java.lang.Exception (unload-unit loaded -1)))
        (is (thrown? java.lang.Exception (unload-unit loaded 1)))
        (is (thrown? java.lang.Exception (unload-unit transporter 0)))))))

(deftest test-weapons
  (let [spec (loader/load-units (resource "units.xml"))
        unit (make-unit spec 0 :red)  ;Infantry
        ]
    (is (can-attack? unit))
    (is (= 1 (count (available-weapons unit))))
    (doseq [weapon (available-weapons unit)]
     (is (= false (low-ammo? weapon))))))



