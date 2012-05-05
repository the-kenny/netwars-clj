(ns netwars.menus.unit-menu
  (:require [netwars.menus.generic :as generic]
            [netwars.logging :as logging]

            [netwars.aw-game :as aw-game]
            [netwars.game-board :as game-board]
            [netwars.aw-map :as aw-map]))

(defn unit-action-menu [game pos fns]
  (let [board (:board game)
        unit (aw-game/selected-unit game)
        capture? (game-board/capture-possible? board pos)
        attack? (not (empty? (aw-game/attackable-targets game)))]
    ;; TODO: Unit with ranged weapons can't fire right after moving
    (generic/make-action-menu
     [["Capture" (:capture fns) (not capture?)]
      ["Attack"  (:attack  fns) (not attack?)]
      [:separator]
      ["Wait"    (:wait    fns)]
      ["Cancel"  (:cancel  fns)]])))
