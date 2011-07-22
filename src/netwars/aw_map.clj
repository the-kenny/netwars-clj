(ns netwars.aw-map)

(defrecord Coordinate [^int x ^int y])

(defn coord
  ([x y] (Coordinate. x y))
  ([[x y]] (Coordinate. x y)))

(defn distance
  "Manhattan metric distance"
  [c1 c2]
  (+ (Math/abs (- (:x c2) (:x c1)))
     (Math/abs (- (:y c2) (:y c1)))))

;; (defn distance
;;  "Euclidean distance between 2 points"
;;  [p1 p2]
;;  (Math/ceil (Math/sqrt (+ (Math/pow (- (:x p1) (:x p2)) 2)
;;                 (Math/pow (- (:y p1) (:y p2)) 2)))))

(defprotocol Board
  (width [board])
  (height [board])
  (at [board ^Coordinate c])
  (update-board [board c v]))

(defn in-bounds? [^Board b ^Coordinate c]
  (and (< -1 (:x c) (width b))
       (< -1 (:y c) (height b))))


(defrecord TerrainBoard [width height data]
  Board
  (width [t] (:width t))
  (height [t] (:height t))
  (at [t c] (get-in (:data t) [(:x c) (:y c)]))
  (update-board [t c v] (assoc-in t [:data (:x c) (:y c)] v)))

(defn make-terrain-board [[width height] data]
  (TerrainBoard. width height data))

;;; Functions to check for different types of terrain

(defn is-building? [t]
  (and (vector? t)
       (get #{:headquarter :city :base :airport :port :tower :lab} (first t))))

(defn is-terrain? [t]
  (get #{:plain :street :bridge :segment-pipe :river :beach :wreckage :pipe
         :mountain :forest :water :reef} t))

(defn is-water? [t]
  (get #{:water :reef :beach :bridge} t))

(defn is-ground? [t]
  (not (is-water? t)))
