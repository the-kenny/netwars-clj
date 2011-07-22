(ns netwars.test.damagetable
  (:use clojure.test
        netwars.damagetable))

(deftest test-damagetable
  (let [table (load-damagetable "resources/damagetable.xml")]
    (is (= {:damage 30} (get-damage table :infantry :t-copter)))
    (is (= {:damage 195 :alt-damage 75}
           (get-damage table :megatank :rockets)))
    (is (= {:damage 35 :alt-damage 35}
           (get-damage table :mech :t-copter)))))
