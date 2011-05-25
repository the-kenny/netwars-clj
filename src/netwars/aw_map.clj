(ns netwars.aw-map)

(defrecord Coordinate [^int x ^int y])

(defn coord
  ([x y] (Coordinate. x y))
  ([[x y]] (Coordinate. x y)))

(defprotocol Board
  (width [board])
  (height [board])
  (in-bounds? [board ^Coordinate c])
  (element-at [board ^Coordinate c]))

(declare unit-at
         terrain-at)

(defrecord AwMapUnit [id color])
(defrecord AwMap [info
                  dimensions ; [width height]
                  terrain ; list of columns 
                  units]
  Board
  (width [t] (first (:dimensions t)))
  (height [t] (second (:dimensions t)))
  (in-bounds? [t c] (and (< -1 (:x c) (width t))
                         (< -1 (:y c) (height t))))
  (element-at [t c] [(terrain-at t c) (unit-at t c)]))

(defn unit-at [m ^Coordinate c]
  (get (:units m) c))

(defn terrain-at [m c]
  (get-in (:terrain m) [(:x c) (:y c)]))
