(ns netwars.test.aw-game
  (:use clojure.test
        netwars.aw-game))

(def *game* nil)

(use-fixtures :each (fn [f]
                      (binding [*game* (make-game nil
                                                  "maps/7330.aws"
                                                  [:player1 :player2 :player3])]
                        (f))))

(deftest test-aw-game
  (is (instance? netwars.aw_game.AwGame *game*)))

(deftest test-player-functions
  (dosync
   (is (= :player1 @(current-player *game*)))

   (next-player! *game*)                ;Go to next player (:player2)
   (is (= :player2 @(current-player *game*)))

   (next-player! *game*) (next-player! *game*) ;go to :player 3, then :player1
   (is (= :player1 @(current-player *game*))))

  (is (thrown? java.lang.IllegalStateException (next-player! *game*))
      "Throws exception outside of transaction"))
