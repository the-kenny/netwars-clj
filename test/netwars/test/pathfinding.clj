(ns netwars.test.pathfinding
  (:use netwars.pathfinding.a-star
        clojure.test
        [netwars.aw-map :only [coord]])
  (:require netwars.game-creator
            [netwars.game-board :as board]
            [netwars.aw-map :as aw-map]))


(def ^:private board (:board (netwars.game-creator/make-game :game-map "7330.aws")))

(defn ^:private calculate-path [start end]
  (a-star-path board
               (board/reachable-fields board start)
               start
               end))

(let [start (aw-map/coord 18 14)
      end   (aw-map/coord 16 14)]
  (deftest t-black-hole-tank
    (testing "Black Hole tank on the down-right corner"
      (is (= (calculate-path start end)
             [(coord 18 14)
              (coord 18 13)
              (coord 17 13)
              (coord 16 13)
              (coord 16 14)])))))

(let [start (aw-map/coord 3 14)
      end   (aw-map/coord 1 12)]
  (deftest t-orange-star-recon-short
    (testing "Orange Star recon on the bottom-left"
      (let [path (calculate-path start end)]
        (is (= path
               [start
                (coord 2 14)
                (coord 2 13)
                (coord 1 13)
                end]))))))

(let [start (aw-map/coord 3 14)
      end   (aw-map/coord 5 9)]
  (deftest t-orange-star-recon
    (testing "Orange Star recon on the bottom-left"
      (let [path (calculate-path start end)]
        (is (seq path))
        (is (= path
               [start
                (coord 3 13)
                (coord 3 12)
                (coord 4 12)
                (coord 5 12)
                (coord 5 11)
                (coord 5 10)
                end]))))))


;;; Manual testing

(comment
  (let [board (:board (netwars.game-creator/make-game {} "7330.aws"))
        start (aw-map/coord 3 14)
        end (aw-map/coord 0 12)
        r (board/reachable-fields board start)]
    (defn test-a-star []
      (= [#netwars.aw_map.Coordinate{:x 3, :y 14}
          #netwars.aw_map.Coordinate{:x 2, :y 14}
          #netwars.aw_map.Coordinate{:x 2, :y 13}
          #netwars.aw_map.Coordinate{:x 1, :y 13}
          #netwars.aw_map.Coordinate{:x 0, :y 13}
          #netwars.aw_map.Coordinate{:x 0, :y 12}]
         (a-star-path board r start end)))

    (defn test-performance-path []
      (print "Path: ")
      (doseq [c (a-star-path board r start end)] (print " ->" (str "(" (:x c) "," (:y c) ")")))
      (prn)
      (time
       (dotimes [_ 1000] (a-star-path board r start end))))))


(comment
  (let [board (:board (netwars.game-creator/make-game {} "7330.aws"))
        start (aw-map/coord 3 14)
        end (aw-map/coord 5 10)
        r (board/reachable-fields board start)]

    (defn test-path []
      (a-star-path board r start end))

    (defn test-a-star []
      (= [#netwars.aw_map.Coordinate{:x 3, :y 14}
          #netwars.aw_map.Coordinate{:x 2, :y 14}
          #netwars.aw_map.Coordinate{:x 2, :y 13}
          #netwars.aw_map.Coordinate{:x 1, :y 13}
          #netwars.aw_map.Coordinate{:x 0, :y 13}
          #netwars.aw_map.Coordinate{:x 0, :y 12}]
         (a-star-path board r start end)))

    (defn test-performance-path []
      (print "Path: ")
      (doseq [c (a-star-path board r start end)] (print " ->" (str "(" (:x c) "," (:y c) ")")))
      (prn)
      (time
       (dotimes [_ 1000] (a-star-path board r start end))))))
