(ns netwars.test.aw-game
  (:use clojure.test
        netwars.aw-game
        [netwars.aw-map :only [coord]]))

(def *game* nil)

(use-fixtures :each (fn [f]
                      (binding [*game* (make-game nil
                                                  "maps/7330.aws"
                                                  [:player1 :player2 :player3])]
                        (f))))


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

(deftest test-player-functions
  (dosync
   (is (= :player1 @(current-player *game*)))

   (next-player! *game*)                ;Go to next player (:player2)
   (is (= :player2 @(current-player *game*)))

   (next-player! *game*) (next-player! *game*) ;go to :player 3, then :player1
   (is (= :player1 @(current-player *game*))))

  (is (thrown? java.lang.IllegalStateException (next-player! *game*))
      "Throws exception outside of transaction")

  (is (= 4 (count (game-events *game*))))
  (doseq [event (rest (game-events *game*))]
    (is (= :turn-completed (:type event)))))
