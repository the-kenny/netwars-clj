(ns netwars.game-creator
  (:require [netwars.aw-game :as aw-game]
            [netwars.damagetable :as damagetable]
            [netwars.game-board :as board]
            [netwars.aw-player :as player])
  (:use [netwars.map-loader :only [load-map]]
        [netwars.unit-loader :only [load-units]])
  (:import [netwars.aw_game AwGame]))

(def +default-funds+ 1000)
(def +maps-path+ "maps/")

(defn- sort-colors [colors]
  (filter (set colors) [:red :blue :yellow :green :black]))

(defn make-game [info mapsource]
  (let [loaded-map (load-map (str +maps-path+ mapsource))
        unit-spec (load-units "resources/units.xml")
        damagetable (damagetable/load-damagetable "resources/damagetable.xml")
        board (board/generate-game-board loaded-map unit-spec)
        newinfo (assoc info :map (:info loaded-map)
                       :map-name mapsource)
        players (map #(player/make-player %1 %2 +default-funds+)
                     (map #(str "Player " %) (range 1 1000))
                     (sort-colors (-> loaded-map :info :player-colors)))
        initial-event {:type :game-started
                       :info newinfo
                       :initial-board board
                       :unit-spec unit-spec
                       :players players}]
    (AwGame. newinfo
             0                    ;current-player-index
             1                    ;round-counter
             (vec players)
             unit-spec
             damagetable
             board
             nil                  ;current-unit
             [initial-event])))
