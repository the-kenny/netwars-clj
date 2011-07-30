(ns netwars.aw-map
  (:use [clojure.contrib.json :as json]))

(defrecord Coordinate [^int x ^int y]
  json/Write-JSON
  (write-json [obj out]
    (write-json [(:x obj) (:y obj)] out)))

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
      {:plain        {:infantry 1   :mech 1   :tread 1   :tires 2   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :wreckage     {:infantry 1   :mech 1   :tread 1   :tires 2   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :forest       {:infantry 1   :mech 1   :tread 2   :tires 3   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :city         {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :base         {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :bridge       {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 1}
       :headquarter  {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe 1   :hover 1}
       :mountain     {:infantry 2   :mech 1   :tread nil :tires nil :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover 0}
       :pipe         {:infantry 2   :mech nil :tread nil :tires nil :air nil :sea nil :transport nil :oozium nil :pipe 1   :hover nil}
       :reef         {:infantry 2   :mech nil :tread nil :tires nil :air 1   :sea 1   :transport 1   :oozium nil :pipe nil :hover nil}
       :street       {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover nil}
       :water        {:infantry nil :mech nil :trad nil  :tires nil :air 1   :sea 1   :transport 1   :oozium nil :pipe nil :hover nil}
       :silo         {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport nil :oozium 1   :pipe 1   :hover nil}
       :river        {:infantry 2   :mech 1   :tread nil :tires nil :air 1   :sea nil :transport nil :oozium 1   :pipe nil :hover nil}
       :beach        {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport 1   :oozium 1   :pipe nil :hover nil}
       :port         {:infantry 1   :mech 1   :tread 1   :tires 1   :air 1   :sea nil :transport 1   :oozium 1   :pipe nil :hover nil}}]
  (defn movement-costs [terrain type]
    (get-in cost [(if (vector? terrain) (first terrain) terrain) type])))

(defn can-pass? [terrain movement-type]
  (boolean (movement-costs terrain movement-type)))
