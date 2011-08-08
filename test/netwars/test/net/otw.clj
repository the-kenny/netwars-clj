(ns netwars.test.net.otw
  (:use clojure.test
        netwars.net.otw))

(deftest test-encoding
  (is (= (encode-data [1 2 3]) "(1 2 3)"))
  (is (= (encode-data {:foo 42}) "{:foo 42}"))
  (is (= "^{:foo 42} {}" (encode-data (with-meta {} {:foo 42})))))
