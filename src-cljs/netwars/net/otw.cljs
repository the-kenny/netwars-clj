(ns netwars.net.otw
  (:require [cljs.reader :as reader]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.aw-player :as aw-player]
            [netwars.game-board :as board]
            [netwars.aw-game :as aw-game]))

(reader/register-tag-parser! "coord" aw-map/coord)
(reader/register-tag-parser! "netwars.aw_unit.AwUnit" aw-unit/map->AwUnit)
(reader/register-tag-parser! "netwars.aw_player.AwPlayer" aw-player/map->AwPlayer)
(reader/register-tag-parser! "netwars.aw_map.TerrainBoard" aw-map/map->TerrainBoard)
(reader/register-tag-parser! "netwars.game_board.GameBoard" board/map->GameBoard)
(reader/register-tag-parser! "netwars.aw_game.AwGame" aw-game/map->AwGame)

(defn encode-data [data]
  {:pre [(map? data)]}
  (binding [*print-meta* true]
    (pr-str data)))

(defn decode-data [s]
  {:pre [(string? s)]}
  (reader/read-string s))
