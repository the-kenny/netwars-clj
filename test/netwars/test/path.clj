(ns netwars.test.path
  (:use clojure.test
        [netwars.aw-map :only [coord]]
        netwars.path)
  (:import netwars.path.AwPath))

(deftest test-make-path
  (let [coords (map coord [[1 1] [1 2] [1 3]])]
   (is (instance? netwars.path.AwPath (make-path coords)))
   (is (= coords (:coordinates (make-path coords))))
   (is (thrown? java.lang.IllegalArgumentException
                (make-path (conj coords (coord 0 0)))))))

(deftest test-valid-path
  (let [path    (AwPath. (map coord [[1 1] [1 2] [1 3]]))
        invalid (AwPath. (map coord [[1 1] [1 2] [1 4]]))]
    (testing "valid paths"
      (is (valid-path? path))
      (is (valid-path? (AwPath. []))))
    (testing "unconnected paths"
      (is (not (valid-path? invalid)))
      (is (not (valid-path? (AwPath. [1 2])))))))

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
