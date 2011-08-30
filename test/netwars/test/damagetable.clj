(ns netwars.test.damagetable
  (:use clojure.test
        netwars.damagetable))

(deftest test-load-damagetable
  (is (map? (load-damagetable "resources/damagetable.xml")))
  (is (map? (load-damagetable "resources/damagetable.xml"))
      "should return a map with internal names of units as keys")
  (is (= (set (keys (load-damagetable "resources/damagetable.xml")))
         #{:infantry :mech :tank :md-tank :neotank :megatank :recon :missiles
           :artillery :rockets :piperunner :cruiser :submarine :carrier
           :anti-air :battleship :b-copter :stealth :fighter :bomber})))

(deftest test-get-damage
  (let [table (load-damagetable "resources/damagetable.xml")]
    (is (nil? (get-damage table :bomber :fighter))
        "returns nil for non-possible attacks")
    (is (map? (get-damage table :infantry :t-copter)) "returns a map")
    (is (every? #{:damage :alt-damage} (keys (get-damage table :infantry :t-copter)))
        "returns a map with :damage and/or :alt-damage")
    (is (= {:damage 30} (get-damage table :infantry :t-copter)))
    (is (= {:damage 195 :alt-damage 75}
           (get-damage table :megatank :rockets)))
    (is (= {:damage 35 :alt-damage 35}
           (get-damage table :mech :t-copter)))))
