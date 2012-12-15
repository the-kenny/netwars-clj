(ns netwars.game-creator
  (:require [clojure.java.io :refer [file]]
            [netwars.aw-game :as aw-game]
            [netwars.game-board :as board]
            [netwars.aw-player :as player]
            [netwars.damagetable :refer [load-damagetable]]
            [netwars.map-loader :refer [load-map]]
            [netwars.unit-loader :refer [load-units]])
  (:import [netwars.aw_game AwGame]))

(def +default-maps-path+ "maps/")

(defn- sort-colors [colors]
  (filter (set colors) [:red :blue :yellow :green :black]))

(def ^:dynamic *default-game-settings*
  {:funds {:initial 0, :per-building 1000}
   :turn-limit nil                      ;Cut after n days; player with
                                        ;most bases wins
   :fog-of-war false                    ;Not implemented
   })
(def ^:dynamic *default-damagetable* (load-damagetable "resources/damagetable.xml"))
(def ^:dynamic *default-unit-spec*   (load-units "resources/units.xml"))

(defn make-game [& [{:keys [game-map
                            settings
                            damagetable
                            unit-spec
                            game-board]}]]
  (let [game-map (load-map (if (string? game-map)
                             (str +default-maps-path+ game-map)
                             game-map))
        settings (or settings *default-game-settings*)
        damagetable (or damagetable
                        *default-damagetable*)
        unit-spec (or unit-spec
                      *default-unit-spec*)
        game-board (or game-board
                       (board/generate-game-board game-map unit-spec))
        players (map #(player/make-player %1 %2 0)
                     (map #(str "Player " %) (range 1 1000))
                     (sort-colors (-> game-map :info :player-colors)))]
    (aw-game/map->AwGame {:settings settings
                          :players (vec players)
                          :unit-spec unit-spec
                          :damagetable damagetable
                          :board game-board})))

;; (defn make-game [game-map info]
;;   (let [filename (str +maps-path+ game-map)]
;;     (when (.exists (file filename))
;;       (let [loaded-map (load-map filename)
;;             unit-spec (load-units "resources/units.xml")
;;             damagetable (damagetable/load-damagetable )
;;             board (board/generate-game-board loaded-map unit-spec)
;;             settings aw-game/*default-game-settings*
;;             newinfo (assoc info :map (:info loaded-map)
;;                            :map-name game-map)
;;             players (map #(player/make-player %1 %2 0)
;;                          (map #(str "Player " %) (range 1 1000))
;;                          (sort-colors (-> loaded-map :info :player-colors)))]
;;         (-> (aw-game/map->AwGame {:settings settings
;;                                   :players (vec players)
;;                                   :unit-spec unit-spec
;;                                   :damagetable damagetable
;;                                   :board board}))))))
