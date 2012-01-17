(ns netwars.aw-map)

;; This namespace contains various definitions used through the game.
;;
;; - Coordinate is a simple record containing x/y coordinates.
;; - Board is a protocol specifying simple functions for operating on an board with limited width/height
;;
;; The rest of this namespace contains various functions for storing terrain data.


(defrecord Coordinate [^int x ^int y])

(defn coord
  "Creates a coordinate from a x and a y value. x and y are coerced to int"
  ([x y] (Coordinate. x y))
  ([[x y]] (Coordinate. x y)))

(defn coord?
  "Predicate to check if an object is an coordinate"
  [c]
  (instance? Coordinate c))

(when clojure.core/print-method
  (defmethod clojure.core/print-method ::Coordinate [c writer]
           (.write writer (str "[" (:x c) "," (:y c) "]"))))

(defn distance
  "Manhattan metric distance between coordinates"
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

(defn in-bounds?
  "Checks if a coordinate are in the bounds of b. b must implement the Board protocol."
  [b ^Coordinate c]
  (and (< -1 (:x c) (width b))
       (< -1 (:y c) (height b))))


(defrecord TerrainBoard [width height data]
  Board
  (width [t] (:width t))
  (height [t] (:height t))
  (at [t c] (get-in (:data t) [(:x c) (:y c)]))
  (update-board [t c v] (assoc-in t [:data (:x c) (:y c)] v)))

;;; TODO: Specify `data`
(defn make-terrain-board
  "Creates a TerrainBoard with width, height and data.
data must be in a specific format."
  [[width height] data]
  (TerrainBoard. width height data))

;;; Functions to check for different types of terrain

(defn is-building?
  "Predicate to check if a terrain-value is a building.
Building-values have the structure [building color] whereas normal terrains are only keywords."
  [t]
  (and (vector? t)
       (contains? #{:headquarter :city :base :airport :port :tower :lab :silo} (first t))))

(defn is-terrain?
  "Predicate to check if a terrain-value is normal terrain and not a building.
terrain values are ordinary keywords.
More or less the counterpart to `is-building?`"
  [t]
  (contains? #{:plain :street :bridge :segment-pipe :river :beach :wreckage :pipe
         :mountain :forest :water :reef} t))

(defn is-water?
  "Predicate to check if a terrain value is some kind of water.
Mostly useful for drawing of maps."
  [t]
  (contains? #{:water :reef :beach :bridge} t))

(defn is-ground?
    "Predicate to check if a terrain value is some kind of ground. Counterpart to `is-water?`
Mostly useful for drawing of maps."
  [t]
  (and (not (nil? t)) (not (is-water? t))))

(def +movement-cost-table+
  {:plain        {:foot 1   :mechanical 1   :tread 1   :tires 2   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover 1}
   :wreckage     {:foot 1   :mechanical 1   :tread 1   :tires 2   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover 1}
   :forest       {:foot 1   :mechanical 1   :tread 2   :tires 3   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover 1}
   :city         {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover 1}
   :base         {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover 1}
   :bridge       {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover 1}
   :headquarter  {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport nil :oozium 1   :pipe 1   :hover 1}
   :mountain     {:foot 2   :mechanical 1   :tread nil :tires nil :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover nil}
   :pipe         {:foot 2   :mechanical nil :tread nil :tires nil :fly nil :swim nil :transport nil :oozium nil :pipe 1   :hover nil}
   :reef         {:foot nil :mechanical nil :tread nil :tires nil :fly 1   :swim 1   :transport 1   :oozium nil :pipe nil :hover nil}
   :street       {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover nil}
   :water        {:foot nil :mechanical nil :tread nil :tires nil :fly 1   :swim 1   :transport 1   :oozium nil :pipe nil :hover nil}
   :silo         {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport nil :oozium 1   :pipe 1   :hover nil}
   :river        {:foot 2   :mechanical 1   :tread nil :tires nil :fly 1   :swim nil :transport nil :oozium 1   :pipe nil :hover nil}
   :beach        {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport 1   :oozium 1   :pipe nil :hover nil}
   :port         {:foot 1   :mechanical 1   :tread 1   :tires 1   :fly 1   :swim nil :transport 1   :oozium 1   :pipe nil :hover nil}})

(defn movement-costs
  "Returns the movement cost for a movement-type `type` on `terrain`"
  [terrain type]
  (get-in +movement-cost-table+ [(if (vector? terrain) (first terrain) terrain) type]))

(defn can-pass?
  "Predicate to check if `movement-type` can pass `terrain`, e.g. movement-costs are non-nil."
  [terrain movement-type]
  (boolean (movement-costs terrain movement-type)))

(defn can-produce-units? [t]
  (and (sequential? t) (is-building? #{:port :base :airport} (first t))))

(defn defense-value [t]
  )
