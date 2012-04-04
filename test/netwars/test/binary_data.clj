(ns netwars.test.binary-data
  (:use clojure.test
        netwars.binary-data))

(defn- test-buffer [buf & {:keys [size first-byte]}]
  (is (instance? java.nio.ByteBuffer buf))
  (when size
    (is (= (.capacity buf) size) (str "the file has " size " bytes")))
  (when first-byte
    (is (= (.get buf) first-byte) (str "the first byte has the value " first-byte))))

(deftest binary-resource-reading
  (testing "file loading"
    (test-buffer (read-binary-resource "maps/7330.aws")
                 :size 1402
                 :first-byte (int \A)))
  (testing "url loading"
    (test-buffer (read-binary-resource
                  "http://www.advancewarsnet.com/designmaps/mapfiles/7330.aws")
                 :size 1402
                 :first-byte (int \A))))

(defn- make-test-buffer
  ([& vs] (java.nio.ByteBuffer/wrap (byte-array (map byte vs))))
  ([] (make-test-buffer 0x42 0x23 0x00 0x00)))

(deftest value-reading
  (is (= (byte 0x42) (read-byte (make-test-buffer))))
  (is (= (short 16931) (read-dword (make-test-buffer))))
  (is (= (int 1109590016) (read-int32 (make-test-buffer))))
  (is (= "AWS" (read-null-string (make-test-buffer 65 87 83 0 0 0))))
  (is (= "AWS" (read-n-string (make-test-buffer 65 87 83 0 0 0) 3))))
