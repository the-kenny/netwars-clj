(ns netwars.menus.factory-menu
  (:require [netwars.menus.generic :as generic]

            [netwars.aw-game :as aw-game]
            [netwars.game-board :as game-board]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]

            [netwars.tile-drawer :as tile-drawer]
            [netwars.tiles :as tiles]

            [clojure.browser.dom :as dom]))

(defn ^:private make-menu-item [unit]
  (let [canvas (dom/element :canvas)
        table (dom/element :table)]
    (set! (.-width canvas) 16)
    (set! (.-height canvas) 16)
    (tile-drawer/draw-tile (.getContext canvas "2d")
                           tiles/+unit-tiles+
                           [(:color unit) (:internal-name unit)]
                           [16 16]
                           [0 0]
                           nil)
    (let [m (meta unit)]
      (dom/element :table
                   {:class "factory-menu-item"}
                   (dom/element :tr
                    (dom/element :td canvas)
                    (dom/element :td {:class "unit-name"}  (str (:name m) ":"))
                    (dom/element :td {:class "unit-price"} (str (:price m))))))))

(defn factory-menu [game c purchase-fn]
  (let [board (:board game)
        spec (:unit-spec game)
        [factory color] (game-board/get-terrain board c)
        player-funds (:funds (aw-game/current-player game))]
    (generic/make-action-menu
     (for [internal-name (aw-unit/factory-units spec factory)
           :let [unit (aw-unit/make-unit spec internal-name color)]]
       [(make-menu-item unit)
        #(purchase-fn game c unit)
        (< player-funds (:price (meta unit)))]))))
