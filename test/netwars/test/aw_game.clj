(ns netwars.test.aw-game
  (:use clojure.test
        midje.sweet
         netwars.aw-game
        [netwars.path :only [make-path]]
        [netwars.aw-map :only [coord width height at is-building? buildings]]
        [netwars.aw-unit :only [is-unit? main-weapon alt-weapon]]
        [netwars.game-creator :only [make-game]])
  (:require [netwars.game-board :as board]
            [netwars.aw-player :as player]))

(def +aw-test-map+ "maps/7330.aws")

(def ^:dynamic *game* nil)
(defn- make-test-game []
  (make-game nil +aw-test-map+))

(use-fixtures :each (fn [f]
                      (binding [*game* (make-test-game)]
                        (f))))

(background (around :facts (binding [*game* (make-test-game)] ?form)))

(deftest test-aw-game
  (is (instance? netwars.aw_game.AwGame *game*))
  (is (= :game-started (-> *game* :moves first :type))))

(deftest test-action-logging
  (is (thrown? java.lang.Exception
               (log-event *game* {:type 42})))
  (is (thrown? java.lang.Exception
               (log-event *game* {})))

  (is (= 1 (count (game-events *game*))))

  (is (= {:type :foobar}
         (second (game-events (log-event *game* {:type :foobar}))))))

(fact (:players *game*) => (has every? player/is-player?))
(fact (current-player *game*) => player/is-player?)

(facts "about next-player"
  (current-player *game*)    => (contains {:color :red})
  ;; TODO: Check if return val is last player
  (-> *game*
      next-player
      current-player) => (contains {:color :blue})
  (-> *game*
      next-player
      next-player
      current-player) => (contains {:color :black})
  (-> *game*
      next-player
      next-player
      next-player
      current-player) => (contains {:color :red})
  (-> *game*
      next-player
      game-events
      last)           => (contains {:type :turn-completed}))

(facts "about get-player"
  (get-player *game* :red)   => player/is-player?
  (get-player *game* :red)   => (current-player *game*)
  (get-player *game* :blue)  => (contains {:color :blue})
  (get-player *game* :black) => (contains {:color :black}))

(facts "about update-player"
  (-> *game*
      (update-player
                     (:color (current-player *game*))
                     player/spend-funds
                     1000)
      (current-player)) => (contains {:color :red, :funds 0})


  (let [*game* (-> *game* next-player next-player)]
      (-> (update-player *game*
                      (:color (current-player *game*))
                      player/spend-funds
                      500)
          (current-player))) => (contains {:color :black, :funds 500}))

(facts "about remove-player"
  (let [*game* (remove-player *game* :black)]
    (get-player *game* :black) => nil

    (map :color (:players *game*)) =not=> (contains :black)

    (doseq [[c u] (-> *game* :board :units)]
      (fact (:color u) =not=> :black))

    (let [terrain-board (-> *game* :board :terrain)]
      (buildings terrain-board) => (has every? (fn [[_ [_ color]]]
                                                 (not= color :black))))))

(fact "about misusage of remove-player"
  ;; Can't remove current player
  (remove-player *game* (:color (current-player *game*))) => throws)


(facts "about current-round"
  (current-round *game*) => 1
  ;; Go to next round (skip N players)
  (-> (reduce (fn [game _]
                (next-player game))
              *game*
              (range (player-count *game*)))
      (current-round)) => 2)

(defn check-attack-event [attack-event
                          from to
                          att-internal vic-internal]
  (is (boolean attack-event))
  (is (= from (:from attack-event)))
  (is (= to (:to attack-event)))
  (is (= att-internal (-> attack-event :attacker :internal-name)))
  (is (= vic-internal (-> attack-event :victim :internal-name))))

(deftest test-perform-attack
  (let [artillery (coord 1 11)
        infantry (coord 1 13)]
    (dosync
     (is (= 10 (:hp (board/get-unit (:board *game*) artillery))))
     (is (= 10 (:hp (board/get-unit (:board *game*) infantry))))
     (let [*game* (perform-attack *game* artillery infantry)]
       (is (= 10  (:hp (board/get-unit (:board *game*) artillery))))
       (is (= 5.0 (:hp (board/get-unit (:board *game*) infantry))))
       (check-attack-event (first (filter #(= :attack (:type %))
                                          (game-events *game*)))
                           artillery infantry
                           :artillery :infantry)))))

(deftest test-counter-attack
  ;; Add a new infantry next to another for testing
  (let [infantry (coord 1 13)
        infantry2 (coord 2 13)
        *game* (update-in *game* [:game] board/add-unit infantry2
                          (board/get-unit (:board *game*) infantry))]

    (is (= 10 (:hp (board/get-unit (:board *game*) infantry))))
    (is (= 10 (:hp (board/get-unit (:board *game*) infantry2))))
    (let [*game* (perform-attack *game* infantry infantry2)]
      (is (contains? #{9.0 8.0} (:hp (board/get-unit (:board *game*) infantry))))
      (is (contains? #{4.0 5.0} (:hp (board/get-unit (:board *game*) infantry2))))
      (check-attack-event (first (filter #(= :attack (:type %))
                                         (game-events *game*)))
                          infantry infantry2
                          :infantry :infantry)
      (check-attack-event (first (filter #(= :counter-attack (:type %))
                                         (game-events *game*)))
                          infantry2 infantry
                          :infantry :infantry))))

(fact "about ammo usage in perform-attack"
  (let [artillery-c (coord 1 11)
        infantry-c (coord 1 13)
        artillery (board/get-unit (:board *game*) artillery-c)
        original-ammo (:ammo (main-weapon artillery))
        new-ammo (-> *game*
                     (perform-attack artillery-c infantry-c)
                     :board
                     (board/get-unit artillery-c)
                     main-weapon
                     :ammo)]
    (- original-ammo new-ammo) => 1))

(deftest test-fuel-costs
  (let [coords (map coord [[1 13] [2 13] [2 12] [3 12]])
        path (make-path coords)]
    (is (integer? (fuel-costs *game* path)))
    (is (< 0 (fuel-costs *game* path)))
    (is (= 3 (fuel-costs *game* path)) "This path costs 3")
    (is (thrown? java.lang.Exception
                 (fuel-costs *game* (make-path (rest coords))))
        "Throws when there is no unit on the first coordinate")))

(deftest test-select-unit
  (testing "without a transaction"
    (is (thrown? java.lang.Exception
                 (select-unit *game* (coord 1 13)))))
  (let [c (coord 1 13)]
    (is (= (-> *game* (select-unit c) :current-unit) c))))

(deftest test-selected-coordinate
  (is (nil? (selected-coordinate *game*)))
  (let [c (coord 1 13)]
   (is (= c (-> *game* (select-unit c) selected-coordinate)))))

(deftest test-selected-unit
  (testing "on a clean game"
    (is (nil? (selected-unit *game*)) ":current-unit should be nil for a clean game"))
  (testing "after select-unit"
    (is (not (nil? (-> *game* (select-unit (coord 1 11)) selected-unit)))
        ":current-unit should be non-nil after select-unit")))

(deftest test-deselect-unit
  (testing "without a transaction"
    (is (thrown? java.lang.Exception
                 (deselect-unit *game*))))
  (let [c (coord 1 13)
        *game* (select-unit *game* c)]
    (is (= c (deselect-unit *game*)))
    (is (= nil (selected-unit *game*)))))

(deftest test-movement-range
  (let [c (coord 1 13)
        *game* (select-unit *game* c)
        r (movement-range *game*)]
    (is (set? r))
    (is (every? #(instance? netwars.aw_map.Coordinate %) r))
    (is (= r (board/reachable-fields (-> *game* :board)
                                     (selected-coordinate *game*))))))

(deftest test-move-unit
  (let [from (coord 1 13)
        to   (coord 1 14)
        path (make-path [from to])
        unit (board/get-unit (:board *game*) from)]
    (is (netwars.path/valid-path? path (:board *game*))
        "Make sure path used in tests is valid on board")
    (testing "without transaction"
      (is (thrown? java.lang.Exception
                   (move-unit *game* path))))
    (testing "without :current-unit"
      (is (thrown? java.lang.Exception
                   (move-unit *game* path))))
    (testing "with `to` outside movement-range"
      (is (thrown? java.lang.Exception
                   (move-unit (select-unit *game* from)
                              (make-path [from to (coord 0 0)])))))

    (let [*game* (move-unit (select-unit *game* from) path)]
     (is (= (select-keys unit [:internal-name :color])
            (select-keys (board/get-unit@(:board *game*) to)
                         ;; Test for name and color as fuel changes...
                         [:internal-name :color])) "unit really moved to `to`")
     (is (nil? (board/get-unit (:board *game*) from)) "unit really moved from `from`")
     (let [new-unit (board/get-unit (:board *game*) to)]
       (is (< (:fuel new-unit) (:fuel unit)) "the move used fuel"))
     (is (= :unit-moved (:type (last (game-events *game*)))) "the move was logged"))))

;;; From here on, it's Midje

(let [base-coord (coord 7 13)]
 (facts "about buy-unit"
   (let [old-funds (:funds (current-player *game*))
         *game* (buy-unit *game* base-coord :infantry)]
     (let [u (board/get-unit (:board *game*) base-coord)]
       u                                              => is-unit?
       (:internal-name u)                             => :infantry)
     (< (:funds (current-player *game*)) old-funds)   => true))

 (facts "about misuse of buy-unit"
   ;; Can't buy unit when there's already an unit on this coord
   (-> *game*
       (buy-unit base-coord :infantry)
       (buy-unit base-coord :mech))           => throws
   ;; Can't buy unit with insufficient funds
   (buy-unit *game* base-coord :megatank)     => throws))
