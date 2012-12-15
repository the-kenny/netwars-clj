(ns netwars.game-sourcing
  (:require [netwars.aw-game :as aw-game]
            [netwars.game-creator :as game-creator]))

(defmulti apply-game-event (fn [game event] (println event) (:type event)))

(defmethod apply-game-event :game-started
  [game {:keys [info unit-spec damagetable initial-board players]}]
  (assert (nil? game))
  (game-creator/make-game :game-board initial-board
                          :unit-spec unit-spec
                          :damagetable damagetable
                          ;; TODO: handle info and players
                          ))


