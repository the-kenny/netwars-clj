(ns netwars.game
  (:require [netwars.connection :as connection]
            [netwars.drawing :as drawing]
            [netwars.logging :as logging]
            [netwars.pathfinding :as pathfinding]
            [netwars.unit-info :as unit-info]))

;;; TODO: Substitute with a record
;; {:game-id nil
;;  :info {:map-name nil
;;         :players []}}
(def running-game nil)
(def game-units nil)
(def terrain-image nil)


;;; Game state
(def current-unit-coord nil)         ;Stores the coord of the highlighted unit
(def movement-range nil)
(def current-path nil)

;;; General methods for games

(defn unit-at [c]
  (get game-units c))

;;; TODO: pre/post
(defn move-unit [from to]
  (let [u (unit-at from)]
    (set! game-units (-> game-units
                         (dissoc from)
                         (assoc to u)))))

(defn request-select-unit [c]
  (connection/send-data {:type :select-unit
                         :coordinate c}))

(defn request-deselect-unit [c]
  (connection/send-data {:type :deselect-unit
                         :coordinate c}))

(defn unit-clicked [[x y] unit]
  (unit-info/show-unit-info unit)
  (cond
   (nil? current-unit-coord) (request-select-unit [x y])
   (= current-unit-coord [x y]) (request-deselect-unit [x y])))

(defn terrain-clicked [[x y]]
  (when current-unit-coord
    (if (get movement-range [x y])
      (connection/send-data {:type :move-unit
                             :path (pathfinding/elements current-path)})
      (request-deselect-unit current-unit-coord))))

(defn clicked-on [[x y]]
  (let [unit (unit-at [x y])]
    (cond
     unit
     (unit-clicked [x y] unit)
     true
     (terrain-clicked [x y]))))

(defn mouse-moved [[x y]]
  (when running-game
    (when (and current-path (contains? movement-range [x y]))
      (pathfinding/update-path! current-path [x y]))
    (logging/log "Mouse moved: [" x " " y "]")))


;;; Movement Range

(defmethod connection/handle-response :select-unit [message]
  (set! current-unit-coord (:coordinate message))
  (set! current-path (pathfinding/make-path (:coordinate message))))

(defmethod connection/handle-response :deselect-unit [message]
  (set! current-unit-coord nil)
  (set! movement-range nil)
  (set! current-path nil))

(defmethod connection/handle-response :movement-range [message]
  (set! movement-range (:movement-range message)))

;;; Moving Units

(defmethod connection/handle-response :move-unit [message]
  (if (:valid message)
    (let [from (first (:path message))
          to   (last  (:path message))
          fuel-costs (:fuel-costs message)]
      (move-unit from to)
      (set! game-units (update-in game-units [to :fuel] - fuel-costs)))
    (logging/message "Attempted invalid move."))
  ;; Reset movement-range etc.
  (set! movement-range nil)
  (set! current-unit-coord nil)
  (set! current-path nil))

;;; Handling of responses for new games

(defmethod connection/handle-response :game-data [message]
  (set! running-game {:game-id (:game-id message)
                      :info (:info message)})
  (set! game-units nil)
  (set! terrain-image nil)
  (set! movement-range nil)
  (set! current-unit-coord nil)
  (logging/clear-messages))

(defn join-game [game-id]
  (drawing/load-unit-tiles)              ;Make sure tiles are available
  (connection/send-data {:type :join-game
                         :game-id game-id}))


;;; New game fns

(defn start-new-game [map-name]
  (drawing/load-unit-tiles)              ;Make sure tiles are available
  (connection/send-data {:type :new-game,
                         :map-name map-name}))

(defmethod connection/handle-response :new-game [message]
  (logging/log "New game created!"))

;;; Functions for handling data sent after a :game-data request

(defn request-map-data [m]
  (connection/send-data {:type :map-data
                         :map m}))

(defmethod connection/handle-response :map-data [message]
  ;; (drawing/draw-terrain board-context (get message :map-data))
  (drawing/image-from-base64 (:map-data message) #(set! terrain-image %)))

(defmethod connection/handle-response :unit-data [data]
  (logging/log "got " (count (:units data)) " units")
  (set! game-units (:units data)))

;;; Concrete drawing functions

(defn setup-event-listeners [graphics]
  (drawing/add-move-listener graphics mouse-moved))

(defn draw-game [graphics]
  (when (and running-game game-units terrain-image)
    (drawing/clear graphics)
    ;; Draw the map
    (drawing/draw-terrain-image graphics terrain-image)
    ;; Draw the units
    (doseq [[c u] game-units]
      (drawing/draw-unit-at graphics u c))
    ;; Draw the movement-range
    (doseq [c movement-range]
      (drawing/highlight-square graphics c :color "rgba(255, 0, 0, 0.4)"))
    ;; Draw the current path
    (when current-path
      (drawing/draw-path graphics current-path))

    ;; This needs to be re-added after every clear. Dumb kinetic...
    (let [canvas (:canvas graphics)]
      (drawing/add-click-listener graphics
                                  [0 0] (.width canvas) (.height canvas)
                                  #(-> % drawing/canvas->map clicked-on)))))
