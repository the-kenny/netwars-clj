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

;;; Functions to check for different types of terrain

(defn is-building? [t]
  (get #{:headquarter :city :base :airport :port :tower :lab} t))

(defn is-terrain? [t]
  (get #{:plain :street :bridge :segment-pipe :river :beach :wreckage :pipe
         :mountain :forest :water :reef} t))

(defn is-water? [t]
  (get #{:water :reef :beach :bridge} t))

(defn is-ground? [t]
  (not (is-water? t)))
