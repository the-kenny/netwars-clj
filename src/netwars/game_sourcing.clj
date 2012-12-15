(ns netwars.game-sourcing
  (:require [netwars.aw-game :as aw-game]
            [netwars.game-creator :as game-creator]))

(defmulti apply-game-event (fn [game event] (:type event)))

(defmethod apply-game-event :game-started
  [game {:keys [settings unit-spec damagetable initial-board players]}]
  (assert (nil? game))
  (-> (game-creator/make-game :game-board initial-board
                              :unit-spec unit-spec
                              :damagetable damagetable
                              :settings settings
                              :players players
                              ;; TODO: handle info
                              )
      (aw-game/start-game)))

(defmethod apply-game-event :turn-completed
  [game {:keys [player]}]
  (let [game* (aw-game/next-player game)]
    (assert (= player (aw-game/current-player game)))
    (assert (not= player (aw-game/current-player game*)))
    game*))


