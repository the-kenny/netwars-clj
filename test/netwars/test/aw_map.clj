(ns netwars.test.aw-map
  (:use netwars.aw-map
        clojure.test
        midje.sweet
        [netwars.aw-unit :only [+movement-types+]]))

(deftest Coordinate
  (let [c (coord 2 3)]
    (is (= (:x c) 2) "has an x component of 2")
    (is (= (:y c) 3) "has an y component of 3")))

(deftest test-coordinate?
  (is (coord? (coord 1 2)) "returns true for a Coordinate")
  (is (not (coord? 42)) "returns false for everything else"))

(deftest test-distance
  (is (= 3 (distance (coord 0 0) (coord 2 1)))))

(facts "about in-bounds?"
  (in-bounds? ...board... (coord 10 10)) => true
  (provided (width ...board...)          => 30
            (height ...board...)         => 20))

;;; Given map:
;;;  -------
;;; | w p w |
;;; | w w w |
;;;  -------
(deftest TerrainBoard
  (let [m (make-terrain-board [3 2] [[:water :water]
                                     [:plain :water]
                                     [:water :water]])]
    (is (= (width m) 3) "has a width of 3")
    (is (= (height m) 2) "has a height of 2")

    (doseq [[x y] [[-1 0] [3 0] [0 2]]]
      (is (not (in-bounds? m (coord x y)))))
    (doseq [[x y] [[0 0] [1 1] [2 1]]]
      (is (in-bounds? m (coord x y))))

    (is (= :water (at m (coord 0 0))))
    (is (= :plain (at m (coord 1 0))))

    (is (= :foobar (at (update-board m (coord 0 0) :foobar) (coord 0 0))))))

(facts "about is-building?"
  (doseq [t #{:headquarter :city :base :airport :port :tower :lab :silo}]
   (is-building? [t ...color...]) => true)
  (is-building? ...any...)        => false)

(facts "about is-terrain?"
  (doseq [t #{:plain :street :bridge :segment-pipe :river :beach :wreckage :pipe :mountain :forest :water :reef}]
    (is-terrain? t) => true))

(facts "about is-water?"
  (doseq [t #{:water :reef :beach :bridge}]
    (is-water? t)       => true)
  (is-water? ...any...) => false)

(facts "about is-ground?"
  (is-ground? ...ground...)          => true
  (provided (is-water? ...ground...) => false))

(deftest test-movement-cost-table-integrity
  (doseq [[terr costs] +movement-cost-table+]
    (is (or (is-terrain? terr)
            (is-building? [terr nil])))
    (is (every? +movement-types+ (keys costs)))
    (is (every? #(or (nil? %) (> % 0)) (vals costs)))))

(facts "about can-produce-units?"
  (can-produce-units? [:base ...color...])    => true
  (can-produce-units? [:port ...color...])    => true
  (can-produce-units? [:airport ...color...]) => true
  (can-produce-units? ...any...)              => false)

(fact "about defense-value"
  (defense-value ...terrain...)           => integer?
  (defense-value [:headquarter :white])   => 3
  (defense-value [:headquarter anything]) => 4)
