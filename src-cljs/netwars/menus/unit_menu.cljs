(ns netwars.menus.unit-menu
  (:require [netwars.menus.generic :as generic]
            [netwars.logging :as logging]

            [netwars.aw-game :as aw-game]
            [netwars.game-board :as game-board]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]))

(defn unit-action-menu
  "Generate a menu with all the actions the current unit (or `unit')
  can do. Last parameter `unit' is optional. Defaults to the
  selected-unit of `game'."
  [game pos fns & [unit]]
  (let [board (:board game)
        unit (or unit (aw-game/selected-unit game))
        capture? (game-board/capture-possible? board pos)
        attack? (and (not (empty? (aw-game/attackable-targets game)))
                     (if (aw-unit/ranged-weapon? (aw-unit/main-weapon unit))
                       (not (:moved unit))
                       true))]
    (generic/make-toggle-menu
     [["Wait"    (:wait    fns)]
      ["Cancel"  (:cancel  fns)]
      [:separator]
      ["Attack"  (:attack  fns) (not attack?)]
      ["Capture" (:capture fns) (not capture?)]])))













