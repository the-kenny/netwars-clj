(ns netwars.test.path
  (:use clojure.test
        [netwars.aw-map :only [coord]]
        netwars.path
        [netwars.test.game-board :only [make-testboard]]))

(deftest test-make-path
  (is (path? (make-path (map coord [[1 1] [1 2] [1 3]])))))

(deftest test-valid-path
  (let [path (make-path (map coord [[1 1] [1 2] [1 3]]))]
    (is (not (valid-path? (make-path []))))
    (is (not (valid-path? (make-path (map coord [[1 1]])))))
    (testing "valid paths"
      (is (valid-path? path)))
    (testing "general validity"
      (testing "too short paths"
        (is (not (valid-path? (map coord [[1 1] [1 2] [1 4]]))))
        (is (not (valid-path? [1 2]))))
      (is (not (valid-path? (map coord [[1 1] [1 2] [1 1]])))
          "Fields are unique in path"))
    (testing "with optional board-argument"
      (let [board (make-testboard)
            valid (map coord [[1 11] [1 12] [1 13]])]
        (is (not (valid-path? (rest valid) board))
            "should return false when start coordinate is empty")
        (is (not (valid-path? valid board))
            "should return false when last coordinate is not empty")
        (is (not (valid-path? (make-path (map coord (for [y (range 13 999)]
                                                      [1 y])))
                              board))
            "should return false when the path is longer than remaining fuel or movement-range")
        (testing "if fields in path are traversable"
         (is (not (valid-path? (make-path (map coord [[1 11] [1 12] [1 13]]))
                               board))
             "BM Artillery can't traverse field with OS infantry on it")
         (is (not (valid-path? (make-path (map coord [[1 11] [1 10]]))
                               board))
             "BM Artillery can't traverse mountain terrain")
         (is (valid-path? (make-path (map coord [[1 11] [1 12] [2 12] [3 12]]))
                          board))
         "BM Artillery can traverse plain and street")))))

(deftest test-sequence-interface
  (let [coords (map coord [[1 1] [1 2] [1 3]])]
    (testing "first"
     (is (= (coord 1 1) (first (make-path coords)))))
    (testing "last"
     (is (= (coord 1 3) (last (make-path coords)))))
    (testing "map"
      (is (= coords (map identity (make-path coords)))))))
