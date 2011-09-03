(ns netwars.test.path
  (:use clojure.test
        [netwars.aw-map :only [coord]]
        netwars.path))

(deftest test-make-path
  (let [coords (map coord [[1 1] [1 2] [1 3]])]
   (is (instance? netwars.path.AwPath (make-path coords)))
   (is (= coords (:coordinates (make-path coords))))
   (is (thrown? java.lang.IllegalArgumentException
                (make-path (conj coords (coord 0 0)))))))

(deftest test-get-coordinates
  (let [coords (map coord [[1 1] [1 2] [1 3]])]
   (is (= coords (get-coordinates (make-path coords))))))

(deftest test-sequence-interface
  (let [coords (map coord [[1 1] [1 2] [1 3]])]
    (testing "first"
     (is (= (coord 1 1) (first (make-path coords)))))
    (testing "last"
     (is (= (coord 1 3) (last (make-path coords)))))
    (testing "map"
      (is (= coords (map identity (make-path coords)))))))
