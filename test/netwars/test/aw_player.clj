(ns netwars.test.aw-player
  (:use clojure.test
        netwars.aw-player))

(deftest test-make-player
  (is (instance? netwars.aw_player.AwPlayer (make-player "foo" :red 1000)))
  (is (= "foo" (:name (make-player "foo" :red 1000))))
  (is (= 1000 (:funds (make-player "foo" :red 1000))))
  (is (= :red (:color (make-player "foo" :red 1000)))))

(deftest test-spending
  (let [player (make-player "the-kenny" :green 1000)]
    (is (can-spend? player 1000))
    (is (not (can-spend? player 1001)))
    (is (= 42 (:funds (spend-funds player 958))))))
