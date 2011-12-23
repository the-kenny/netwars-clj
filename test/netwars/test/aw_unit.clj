(ns netwars.test.aw-unit
  (:use netwars.aw-unit
        [clojure.java.io :only [resource]]
        [netwars.aw-unit.loader :as loader]
        clojure.test))

(deftest prototype-finding
  (let [spec (loader/load-units (resource "units.xml"))]
    (is (= (:internal-name (find-prototype spec :id 0)) :infantry)
        "finds prototypes based on :id")
    (is (= (:id (find-prototype spec :internal-name :md-tank)) 1)
        "finds prototypes based on :internal-name")
    (is (nil? (find-prototype spec :id 999))
        "returns nil if it can't find a prototype")
    (is (nil? (find-prototype spec
                            :carries-towel ;; Today is towel-day
                            true))
        "returns nil if the key is unknown")))

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
           (find-prototype spec :internal-name (:internal-name weapon-unit)))
        "Instance contains prototype as meta")

    (testing "weapon properties"
      (is (contains? weapon-unit :weapons))
      (is (> (count (:weapons weapon-unit)) 0))
      (doseq [w (:weapons weapon-unit)]
        (is (every? #{:name :ammo :range :distance} (keys w)))
        (is (instance? String (:name w)))
        (is (or (integer? (:ammo w)) (= :infinity (:ammo w))))
        (is (set? (:range w)))
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
        infantry (make-unit spec 0 :red)  ;Infantry
        ]
    (is (can-transport? transporter))
    (is (= #{:infantry :mech} (transport-types transporter)))

    (testing "loading"
      (is (contains? (transport-types transporter) (:internal-name infantry)))
      (let [loaded (transport-unit transporter infantry)]
        (is (= [] (-> transporter :transport :freight)))
        (is (= infantry (get-in loaded [:transport :freight 0])))))

    (testing "unloading"
      (let [loaded (transport-unit transporter infantry)]
        (is (= [transporter infantry] (unload-unit loaded 0)))
        (is (thrown? java.lang.Exception (unload-unit loaded -1)))
        (is (thrown? java.lang.Exception (unload-unit loaded 1)))
        (is (thrown? java.lang.Exception (unload-unit transporter 0)))))))


(def ^:dynamic *spec* nil)

(use-fixtures :each #(binding [*spec* (loader/load-units (resource "units.xml"))]
                       (%)))

(deftest test-weapon-functions
  (let [infantry (make-unit *spec* 0 :red)
        megatank (make-unit *spec* 10 :red)
        apc (make-unit *spec* 22 :red)]
    ;; Infantry
    (is (has-weapons? infantry) "Infantry has a weapon")
    (is (= 1 (count (available-weapons infantry))) "One available weapon")
    (is (every? #{:main-weapon} (keys (weapons infantry))) "Only the main weapon")
    ;; Metagank
    (is (has-weapons? infantry) "Megatank has weapons")
    (is (= 2 (count (available-weapons megatank))) "Megatank has two weapons")
    (is (every? #{:main-weapon :alt-weapon} (keys (weapons megatank))) "main and alt")
    ;; APC
    (is (not (has-weapons? apc)) "An APC doesn't have weapons")
    (is (empty? (weapons apc)) "weapons returns an empty seq")))

(deftest test-available-weapons
  (is false "No tests written")
  #_(doseq [weapon (available-weapons infantry)]
      (is (= false (low-ammo? (val weapon))))))
