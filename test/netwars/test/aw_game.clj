(ns netwars.test.aw-game
  (:use clojure.test
        netwars.aw-game
        [netwars.game-board :as board]
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

(deftest test-perform-attack!
  (let [artillery (coord 1 11)
        infantry (coord 1 13)]
    (dosync
     (is (= 10 (:hp (board/get-unit @(:board *game*) artillery))))
     (is (= 10 (:hp (board/get-unit @(:board *game*) infantry))))
     (perform-attack! *game* artillery infantry)
     (is (= 10 (:hp (board/get-unit @(:board *game*) artillery))))
     (is (= 5 (:hp (board/get-unit @(:board *game*) infantry)))))))

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
     (is (contains? #{4 5} (:hp (board/get-unit @(:board *game*) infantry2)))))))
