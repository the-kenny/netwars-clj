(ns netwars.test.damagecalculator
  (:use clojure.test
        netwars.damagecalculator
        [netwars.damagetable :as damagetable]
        [netwars.aw-unit.loader :as unit-loader]
        [netwars.aw-unit :as unit]))

(deftest test-defense-value
  (doseq [t [:plain :reef]] (is (= 1 (defense-value t))))
  (is (= 2 (defense-value :forest)))
  (doseq [t [:city :base :airport :port :lab]] (is (= 3 (defense-value [t :red]))))
  (is (= 3 (defense-value [:headquarter :white])))
  (is (= 4 (defense-value [:headquarter :red])))
  (is (= 0 (defense-value :asdfhasiodfjll))))


(deftest test-choose-weapon
  (let [table (damagetable/load-damagetable "resources/damagetable.xml")
        unit-spec (unit-loader/load-units "resources/units.xml")
        infantry (unit/make-unit unit-spec 0 :red)
        tank (unit/make-unit unit-spec 21 :red)
        megatank (unit/make-unit unit-spec 10 :red)]

    (is (contains? (choose-weapon table infantry infantry) :main-weapon))
    (is (contains? (choose-weapon table megatank infantry) :alt-weapon))
    (is (contains? (choose-weapon table megatank tank) :main-weapon))

    ;; bare-megatank is a megatank who has no ammo for the Mega-Cannon
    ;; Is there a better way to assoc a nested value in a seq in a map?
    (let [bare-megatank (assoc megatank :weapons (assoc-in (vec (:weapons megatank))
                                                           [0 :ammo] 0))]
      (is (contains? (choose-weapon table bare-megatank tank) :alt-weapon)))))

(deftest test-damage-calculation
  (let [table (damagetable/load-damagetable "resources/damagetable.xml")
        unit-spec (unit-loader/load-units "resources/units.xml")
        infantry (unit/make-unit unit-spec 0 :red)
        tank (unit/make-unit unit-spec 21 :red)
        artillery (unit/make-unit unit-spec 3 :red)
        megatank (unit/make-unit unit-spec 10 :red)]
    ;; These values are taken from Advance Wars Dual Strike
    (is (= 55.0 (calculate-unrounded-damage table
                                            [infantry :street]
                                            [infantry :street])))
    (is (= 22.0 (calculate-unrounded-damage table
                                            [(assoc infantry :hp 4) :street]
                                            [infantry :street])))
    (is (= 20.0 (round-damage 22.0)))
    (is (= 20.0 (round-damage 18.0)))

    ;; (round-damage 15.0) can return either 10.0 or 20.0.
    ;; Test in a loop for both values
    (doseq [x (range 10)]
      (is (contains? #{20.0 10.0} (round-damage 15.0))))

    (is (= 5 (calculate-damage table
                                [artillery :plain]
                                [infantry [:headquarter :red]])))
    (is (= 2 (calculate-damage table
                               [(assoc infantry :hp 4) :forest]
                               [(assoc infantry :hp 8) :street])))
    (is (= 1 (calculate-damage table
                               [(assoc infantry :hp 2) :forest]
                               [(assoc infantry :hp 8) :street])))))
