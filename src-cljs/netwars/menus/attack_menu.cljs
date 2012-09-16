(ns netwars.menus.attack-menu
  (:require [netwars.menus.generic :as generic]
            [netwars.logging :as logging]

            [netwars.aw-game :as aw-game]
            [netwars.game-board :as game-board]
            [netwars.damagecalculator :as damage]
            [netwars.aw-map :as aw-map]
            [clojure.browser.dom :as dom]))

(defn attack-menu [game target fns]
  (let [board (:board game)
        attacker (aw-game/selected-unit game)
        victim (game-board/get-unit (:board game) target)
        can-attack? (aw-game/attack-possible? game
                                              (aw-game/selected-coordinate game)
                                              target)
        damage (damage/calculate-unrounded-damage
                (:damagetable game)
                [attacker (game-board/get-terrain board
                                                  (aw-game/selected-unit game))]
                [victim (game-board/get-terrain board target)])]
    (assert attacker)
    (assert victim)
    (assert can-attack?)
    (generic/make-toggle-menu
     [[(str "Damage: " damage "%") nil :disabled]
      [:separator]
      ["Attack!" (:attack fns)]
      ["Cancel"  (:cancel fns)]])))
