(ns netwars.net.test.otw
  (:use clojure.test
        [netwars.net.otw :as otw]
        [clojure.java.io :only [resource]])
  (:import javax.imageio.ImageIO))

(deftest test-sendable
  (let [data {:coord (netwars.aw-map/coord 1 2)
              :string "Foo"
              :vec [23 42 1337]
              :set #{1 2 3}
              :uuid (java.util.UUID/randomUUID)
              :image (ImageIO/read (resource "background.png"))}]
    (doseq [[k v] data :let [m {k v}]]
      (is (string? (otw/encode-data m)))
      (is (map? (otw/decode-data (otw/encode-data m)))))))
