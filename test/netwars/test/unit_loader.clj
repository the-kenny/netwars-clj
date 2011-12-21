(ns netwars.test.unit-loader
  (:use clojure.test
        netwars.aw-unit.loader
        [clojure.java.io :only [resource]]
        [clojure.set :only [subset?]]))

(deftest unit-loading
  (is (load-units (resource "units.xml"))
      "loads an unit-spec from a file"))

(deftest unit-spec
  (let [unit-spec (load-units (resource "units.xml"))]
    (is (and (seq? unit-spec)
             (> (count unit-spec)))
        "is a sequence with >0 elements")

    (testing "every unit-prototype in this spec"
      (let [required-keys #{:internal-name :name :hp}]
        (doseq [spec unit-spec]
          (is (map? spec))
          (is (subset? required-keys (-> spec keys set)))

          ;; Weapons
          (doseq [weapon ((juxt :main-weapon :alt-weapon) spec)
                  :when weapon]
            (is (every? #{:name :ammo :range} (keys weapon)))
            (is (set? (:range weapon)) "range is a set of reachable distances")))))))

(deftest prototype-finding
  (let [spec (load-units (resource "units.xml"))]
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
