(ns netwars.aw-map)

(defrecord Coordinate [^int x ^int y])

(defn coord
  ([x y] (Coordinate. x y))
  ([[x y]] (Coordinate. x y)))

(defprotocol Board
  (width [board])
  (height [board])
  (at [board ^Coordinate c]))

(defn in-bounds? [^Board b ^Coordinate c]
  (and (< -1 (:x c) (width b))
       (< -1 (:y c) (height b))))


(defrecord TerrainBoard [width height data]
  Board
  (width [t] (:width t))
  (height [t] (:height t))
  (at [t c] (get-in (:data t) [(:x c) (:y c)])))

(defn make-terrain-board [[width height] data]
  (TerrainBoard. width height data))

