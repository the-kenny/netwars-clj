(ns netwars.test.aw-game
  (:use clojure.test
        midje.sweet
        netwars.aw-game
        [netwars.path :only [make-path]]
        [netwars.game-board :as board]
        [netwars.aw-map :only [coord]]
        [netwars.aw-unit :only [is-unit?]]
        [netwars.aw-player :as player]))

(def ^:dynamic *game* nil)

(def +aw-test-map+ "maps/7330.aws")

(defn- make-test-game []
  (make-game nil +aw-test-map+ [:player1 :player2 :player3]))

(use-fixtures :each (fn [f]
                      (binding [*game* (make-test-game)]
                        (f))))

(background (around :facts (binding [*game* (make-test-game)] ?form)))

(deftest test-aw-game
  (is (instance? netwars.aw_game.AwGame *game*))
  (is (= :game-started (-> *game* :moves deref first :type))))

(deftest test-action-logging
  (is (thrown? java.lang.IllegalStateException
               (log-event! *game* {:type 42})))
  (dosync
   (is (thrown? java.lang.AssertionError
                (log-event! *game* {})))

   (is (= 1 (count (game-events *game*))))
   (log-event! *game* {:type :foobar})
   (is (= {:type :foobar} (second (game-events *game*))))))

(fact (current-player *game*) => player/is-player?)

(facts "about next-player!"
  (dosync
   (current-player *game*)    => (contains {:color :red})
   (next-player! *game*)      => is-player?  ;TODO: Check if return val is last player
   (current-player *game*)    => (contains {:color :blue})
   (next-player! *game*)      => is-player?
   (current-player *game*)    => (contains {:color :black})
   (next-player! *game*)      => is-player?  ;Go back to red
   (current-player *game*)    => (contains {:color :red}))
  (rest (game-events *game*)) => (three-of (contains {:type :turn-completed})))

(defn check-attack-event [attack-event
                          from to
                          att-internal vic-internal]
  (is (boolean attack-event))
  (is (= from (:from attack-event)))
  (is (= to (:to attack-event)))
  (is (= att-internal (-> attack-event :attacker :internal-name)))
  (is (= vic-internal (-> attack-event :victim :internal-name))))

(deftest test-perform-attack!
  (let [artillery (coord 1 11)
        infantry (coord 1 13)]
    (dosync
     (is (= 10 (:hp (board/get-unit @(:board *game*) artillery))))
     (is (= 10 (:hp (board/get-unit @(:board *game*) infantry))))
     (perform-attack! *game* artillery infantry)
     (is (= 10 (:hp (board/get-unit @(:board *game*) artillery))))
     (is (= 5 (:hp (board/get-unit @(:board *game*) infantry))))
     (check-attack-event (first (filter #(= :attack (:type %))
                                       (game-events *game*)))
                         artillery infantry
                         :artillery :infantry))))

(deftest test-counter-attack
  (let [infantry (coord 1 13)
        infantry2 (coord 2 13)]
    (dosync
     ;; Add a new infantry next to another for testing
       (alter (:board *game*) board/add-unit infantry2
            (board/get-unit @(:board *game*) infantry))
     (is (= 10 (:hp (board/get-unit @(:board *game*) infantry))))
     (is (= 10 (:hp (board/get-unit @(:board *game*) infantry2))))
     (perform-attack! *game* infantry infantry2)
     (is (contains? #{9 8} (:hp (board/get-unit @(:board *game*) infantry))))
     (is (contains? #{4 5} (:hp (board/get-unit @(:board *game*) infantry2))))
     (check-attack-event (first (filter #(= :attack (:type %))
                                        (game-events *game*)))
                         infantry infantry2
                         :infantry :infantry)
     (check-attack-event (first (filter #(= :counter-attack (:type %))
                                        (game-events *game*)))
                         infantry2 infantry
                         :infantry :infantry))))

(deftest test-fuel-costs
  (let [coords (map coord [[1 13] [2 13] [2 12] [3 12]])
        path (make-path coords)]
    (is (integer? (fuel-costs *game* path)))
    (is (< 0 (fuel-costs *game* path)))
    (is (= 3 (fuel-costs *game* path)) "This path costs 3")
    (is (thrown? java.lang.IllegalArgumentException
                 (fuel-costs *game* (make-path (rest coords))))
        "Throws when there is no unit on the first coordinate")))

(deftest test-select-unit!
  (testing "without a transaction"
    (is (thrown? java.lang.IllegalStateException
                 (select-unit! *game* (coord 1 13)))))
  (let [c (coord 1 13)]
    (dosync (select-unit! *game* c))
    (is (= c @(:current-unit *game*)))))

(deftest test-selected-coordinate
  (is (nil? (selected-coordinate *game*)))
  (let [c (coord 1 13)]
   (dosync (select-unit! *game* c))
   (is (= c (selected-coordinate *game*)))))

(deftest test-selected-unit
  (testing "on a clean game"
    (is (nil? (selected-unit *game*)) ":current-unit should be nil for a clean game"))
  (testing "after select-unit!"
    (dosync (select-unit! *game* (coord 1 11)))
    (is (not (nil? (selected-unit *game*)))
        ":current-unit should be non-nil after select-unit!")))

(deftest test-deselect-unit!
  (testing "without a transaction"
    (is (thrown? java.lang.IllegalStateException
                 (deselect-unit! *game*))))
  (let [c (coord 1 13)]
   (dosync
    (select-unit! *game* c)
    (is (= c (deselect-unit! *game*)))
    (is (= nil (selected-unit *game*))))))

(deftest test-movement-range
  (let [c (coord 1 13)]
    (dosync (select-unit! *game* c))
    (let [r (movement-range *game*)]
      (is (set? r))
      (is (every? #(instance? netwars.aw_map.Coordinate %) r))
      (is (= r (board/reachable-fields (-> *game* :board deref)
                                       (selected-coordinate *game*)))))))

(deftest test-move-unit!
  (let [from (coord 1 13)
        to   (coord 1 14)
        path (make-path [from to])
        unit (board/get-unit @(:board *game*) from)]
    (is (netwars.path/valid-path? path @(:board *game*))
        "Make sure path used in tests is valid on board")
    (testing "without transaction"
      (is (thrown? java.lang.IllegalStateException
                   (move-unit! *game* path))))
    (dosync
     (testing "without :current-unit"
       (is (thrown? java.lang.IllegalStateException
                    (move-unit! *game* path))))
     (select-unit! *game* from)
     (testing "with `to` outside movement-range"
       (is (thrown? java.lang.IllegalArgumentException
                    (move-unit! *game* (make-path [from to (coord 0 0)]))))))
    (dosync
     (select-unit! *game* from)
     (is (= path (move-unit! *game* path))))
    (is (= (select-keys unit [:internal-name :color])
           (select-keys (board/get-unit @(:board *game*) to)
                        ;; Test for name and color as fuel changes...
                        [:internal-name :color])) "unit really moved to `to`")
    (is (nil? (board/get-unit @(:board *game*) from)) "unit really moved from `from`")
    (let [new-unit (board/get-unit @(:board *game*) to)]
      (is (< (:fuel new-unit) (:fuel unit)) "the move used fuel"))
    (is (= :unit-moved (:type (last (game-events *game*)))) "the move was logged")))

;;; From here on, it's Midje

(facts "about buy-unit!"
  (let [base-coord (coord 7 13)
        old-funds (:funds (current-player *game*))]
    (dosync (buy-unit! *game* base-coord :infantry)) => is-unit?
    (let [u (board/get-unit @(:board *game*) base-coord)]
      u                                              => is-unit?
      (:internal-name u)                             => :infantry)
    (< (:funds (current-player *game*)) old-funds)   => true))
