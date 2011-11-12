(ns netwars.test.path
  (:use clojure.test
        [netwars.aw-map :only [coord]]
        netwars.path
        [netwars.test.game-board :only [make-testboard]])
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
    (is (not (valid-path? (AwPath. []))))
    (is (not (valid-path? (AwPath. (map coord [[1 1]])))))
    (testing "valid paths"
      (is (valid-path? path)))
    (testing "unconnected paths"
      (is (not (valid-path? invalid)))
      (is (not (valid-path? (AwPath. [1 2])))))
    (testing "with optional board-argument"
      (let [board (make-testboard)
            valid (AwPath. (map coord [[1 11] [1 12] [1 13]]))]
        (is (not (valid-path? (rest valid) board))
            "should return false when start coordinate is empty")
        (is (not (valid-path? valid board))
            "should return false when last coordinate is not empty")
        (is (not (valid-path? (make-path (map coord (for [y (range 13 999)]
                                                      [1 y])))
                              board))
            "should return false when the path is longer than remaining fuel or movement-range")
        (testing "if fields in path are traversable"
         (is (not (valid-path? (AwPath. (map coord [[1 11] [1 12] [1 13]]))
                               board))
             "BM Artillery can't traverse field with OS infantry on it")
         (is (not (valid-path? (AwPath. (map coord [[1 11] [1 10]]))
                               board))
             "BM Artillery can't traverse mountain terrain")
         (is (valid-path? (AwPath. (map coord [[1 11] [1 12] [2 12] [3 12]]))
                          board))
         "BM Artillery can traverse plain and street")))))

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
