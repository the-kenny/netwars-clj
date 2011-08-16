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
  (and (not (nil? t)) (not (is-water? t))))

(let [cost
      {:plain        {:foot 1   :mechanical 1   :tread 1   :tires 2   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :wreckage     {:foot 1   :mechanical 1   :tread 1   :tires 2   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :forest       {:foot 1   :mechanical 1   :tread 2   :tires 3   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :city         {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :base         {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :bridge       {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :headquarter  {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe 1   :hover 1}
       :mountain     {:foot 2   :mechanical 1   :tread nil :tires nil :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 0}
       :pipe         {:foot 2   :mechanical nil :tread nil :tires nil :air nil :sea nil :transport nil :oozium nil :pipe 1   :hover nil}
       :reef         {:foot nil :mechanical nil :tread nil :tires nil :air 1   :sea 1   :transport 1   :oozium nil :pipe nil :hover nil}
       :street       {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover nil}
       :water        {:foot nil :mechanical nil :trad nil  :tires nil :air 1   :sea 1   :transport 1   :oozium nil :pipe nil :hover nil}
       :silo         {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe 1   :hover nil}
       :river        {:foot 2   :mechanical 1   :tread nil :tires nil :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover nil}
       :beach        {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport 1   :oozium 1   :pipe nil :hover nil}
       :port         {:foot 1   :mechanical 1   :tread 1   :tires 1   :air 1   :sea nil :transport 1   :oozium 1   :pipe nil :hover nil}}]
  (defn movement-costs [terrain type]
    (get-in cost [(if (vector? terrain) (first terrain) terrain) type])))

(defn can-pass? [terrain movement-type]
  (boolean (movement-costs terrain movement-type)))
