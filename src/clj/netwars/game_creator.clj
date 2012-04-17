(ns netwars.game-creator
  (:require [netwars.aw-game :as aw-game]
            [netwars.damagetable :as damagetable]
            [netwars.game-board :as board]
            [netwars.aw-player :as player])
  (:use [netwars.map-loader :only [load-map]]
        [netwars.unit-loader :only [load-units]])
  (:import [netwars.aw_game AwGame]))

(def +default-funds+ 1000)

(defn- sort-colors [colors]
  (filter (set colors) [:red :blue :yellow :green :black]))

(defn make-game [info mapsource]
  (let [loaded-map (load-map mapsource)
        unit-spec (load-units "resources/units.xml")
        damagetable (damagetable/load-damagetable "resources/damagetable.xml")
        board (board/generate-game-board loaded-map unit-spec)
        newinfo (assoc info :map mapsource)
        players (map #(player/make-player %1 %2 +default-funds+)
                     (map #(str "Player " %) (range 1 1000))
                     (sort-colors (-> loaded-map :info :player-colors)))
        initial-event {:type :game-started
                       :info newinfo
                       :loaded-map loaded-map
                       :unit-spec unit-spec
                       :players players}]
    (AwGame. newinfo
             (ref 0)                    ;current-player-index
             (ref 1)                    ;round-counter
             (ref (vec players))
             unit-spec
             damagetable
             (ref board)
             (ref nil)                  ;current-unit
             (ref [initial-event]))))