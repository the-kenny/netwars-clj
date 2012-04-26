(ns netwars.aw-map)

;; This namespace contains various definitions used through the game.
;;
;; - Coordinate is a simple record containing x/y coordinates.
;; - Board is a protocol specifying simple functions for operating on an board with limited width/height
;;
;; The rest of this namespace contains various functions for storing terrain data.


;;; TODO: Replace condp with case when Clojurescript supports it
(deftype Coordinate [x y]
  clojure.lang.Indexed
  (nth [s i not-found]
    (condp = i
      0 x, 1 y
      not-found)
    ;; (case i
    ;;   0 x
    ;;   1 y
    ;;   not-found)
    )
  (nth [s i]
    (nth s i nil))

  clojure.lang.ILookup
  (valAt [s k not-found]
    ;; (case k
    ;;   :x x, :y y
    ;;   not-found)
    (condp = k
      :x x, :y y
      not-found))
  (valAt [s k]
    ;; (case k
    ;;   :x x, :y y
    ;;   nil)
    (condp = k
      :x x, :y y))

  Object
  (toString [[x y]] (str "(coord " x " " y ")"))
  (hashCode [s] (* (+ (* 17 (hash x)) (hash y)) 54))
  (equals [s o]
    (and
     (instance? Coordinate o)
     (= x (:x o))
     (= y (:y o)))))

(defn coord
  "Creates a coordinate from a x and a y value. x and y are coerced to int"
  ([x y] (Coordinate. x y))
  ([[x y]] (Coordinate. x y)))

(defn coord?
  "Predicate to check if an object is an coordinate"
  [c]
  (instance? Coordinate c))

(when clojure.core/print-method
  (defmethod clojure.core/print-method ::Coordinate [[x y] ^java.io.Writer writer]
    (.write writer (str "[" y "," x "]"))))

(defn distance
  "Manhattan metric distance between coordinates"
  [[x1 y1] [x2 y2]]
  (+ (Math/abs (- x2 x1))
     (Math/abs (- y2 y1))))

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
  [b ^Coordinate [x y]]
  (and (< -1 x (width b))
       (< -1 y (height b))))

(defrecord TerrainBoard [width height data]
  Board
  (width [t] (:width t))
  (height [t] (:height t))
  (at [t [x y]] (get-in t [:data x y]))
  (update-board [t ^Coordinate [x y] v] (assoc-in t [:data x y] v)))

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

(def +building-capture-points+ 20)

(defn capture-building [building points new-color]
  {:pre [(is-building? building)]
   :post [(is-building? %)]}
  (let [[t c val] building]
    (cond
     (nil? val) (if (= points +building-capture-points+)
                  [t new-color]
                  [t c (- +building-capture-points+ points)])
     (<=    (- val points) 0) [t new-color]
     (pos?  (- val points))   [t c (- val points)])))

(defn capture-points [building]
  {:pre [(is-building? building)]}
  (let [[t c p] building]
    (or p +building-capture-points+)))

(defn reset-capture-points [building]
  {:pre [(is-building? building)]}
  (let [[t c p] building]
    [t c]))

(defn is-terrain?
  "Predicate to check if a terrain-value is normal terrain and not a building.
terrain values are ordinary keywords.
More or less the counterpart to `is-building?`"
  [t]
  (contains? #{:plain :street :bridge :segment-pipe :river :beach :wreckage :pipe :mountain :forest :water :reef} t))

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

(defn buildings [board]
  {:pre [(instance? TerrainBoard board)]}
  (filter #(is-building? (second %)) (for [x (range (width board))
                                           y (range (height board))
                                           :let [c (coord x y)]]
                                       [c (at board c)])))

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
  (and (sequential? t) (is-building? t) (contains? #{:port :base :airport} (first t))))

(def ^:private +defense-values+
  {:plain 1
   :reef  1
   :forest 2
   :city 3
   :base 3
   :airport 3
   :port 3
   :lab 3
   :headquarter 4
   :mountain 3
   :silo 3})

(defn defense-value [terrain]
  (let [[t c] (cond
               (keyword? terrain) [terrain]
               (is-building? terrain) terrain
               true [])]
    (get +defense-values+ t 0)))
