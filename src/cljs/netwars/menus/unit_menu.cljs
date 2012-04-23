(ns netwars.menus.unit-menu
  (:require [netwars.menus.generic :as generic]
            [netwars.logging :as logging]

            [netwars.aw-game :as aw-game]
            [netwars.game-board :as game-board]
            [netwars.aw-map :as aw-map]))

(defn unit-action-menu [game pos fns]
  (let [board (:board game)
        unit (aw-game/selected-unit game)
        capture? (game-board/capture-possible? board pos)]
    (generic/make-action-menu
     [["Capture" (:capture fns) (not capture?)]
      #_["Attack"  (:attack fns)  :disabled]
      [:separator]
      ["Wait"    (:wait fns)]])))
