(ns netwars.game-creator
  (:require [netwars.aw-game :as aw-game]
            [netwars.damagetable :as damagetable]
            [netwars.game-board :as board]
            [netwars.aw-player :as player]
            [clojure.java.io :refer [file]])
  (:use [netwars.map-loader :only [load-map]]
        [netwars.unit-loader :only [load-units]])
  (:import [netwars.aw_game AwGame]))

(def +maps-path+ "maps/")

(defn- sort-colors [colors]
  (filter (set colors) [:red :blue :yellow :green :black]))

(defn make-game [info mapsource]
  (let [filename (str +maps-path+ mapsource)]
    (when (.exists (file filename))
      (let [loaded-map (load-map filename)
            unit-spec (load-units "resources/units.xml")
            damagetable (damagetable/load-damagetable "resources/damagetable.xml")
            board (board/generate-game-board loaded-map unit-spec)
            settings aw-game/*default-game-settings*
            newinfo (assoc info :map (:info loaded-map)
                           :map-name mapsource)
            players (map #(player/make-player %1 %2 0)
                         (map #(str "Player " %) (range 1 1000))
                         (sort-colors (-> loaded-map :info :player-colors)))]
        (-> (aw-game/map->AwGame {:settings settings
                                  :players (vec players)
                                  :unit-spec unit-spec
                                  :damagetable damagetable
                                  :board board}))))))
