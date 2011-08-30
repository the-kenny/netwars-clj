(ns netwars.game
  (:require [netwars.connection :as connection]
            [netwars.drawing :as drawing]
            [netwars.logging :as logging]))

;;; TODO: Substitute with a record
;; {:game-id nil
;;  :info {:map-name nil
;;         :players []}}
(def running-game (atom nil))
(def game-units (atom nil))
(def terrain-image (atom nil))


;;; Game state
(def current-unit nil)         ;Stores the coord of the highlighted unit
(def movement-range nil)

;;; General methods for games

(defn unit-at [c]
  (get @game-units c))

;;; TODO: pre/post
(defn move-unit [from to]
  (let [u (unit-at from)]
    (swap! game-units dissoc from)
    (swap! game-units assoc to u)))

(defn clicked-on [[x y]]
  (logging/log "clicked on: " x "/" y)
  (when-let [u (unit-at [x y])]
    (logging/message "Unit: " (name (:internal-name u)))
    (connection/send-data {:type :movement-range
                           :coordinate [x y]}))
  (when (get movement-range [x y])
    (connection/send-data {:type :move-unit
                           :from current-unit
                           :to [x y]})))

(defn unit-clicked [[x y] unit]
  (logging/message "Unit: " (name (:internal-name unit)) " (" (name (:color unit)) ") "
                    (:hp unit) "hp"))


;;; Movement Range

(defmethod connection/handle-response :movement-range [message]
  (set! movement-range (:movement-range message))
  (set! current-unit (:coordinate message)))

;;; Moving Units

(defmethod connection/handle-response :move-unit [message]
  (move-unit (:from message) (:to message))
  ;; Reset movement-range
  (set! movement-range nil)
  (set! current-unit nil))

;;; Handling of responses for new games

(defmethod connection/handle-response :game-data [message]
  (reset! running-game {:game-id (:game-id message)
                        :info (:info message)}))

(defn join-game [game-id]
  (connection/send-data {:type :join-game
                                :game-id game-id}))


;;; New game fns

(defn start-new-game [map-name]
  (connection/send-data {:type :new-game,
                                :map-name map-name}))

(defmethod connection/handle-response :new-game [message]
  (logging/log "New game created!"))

;;; Functions for handling data sent after a :game-data request

(defn request-map-data [m]
  (connection/send-data {:type :request-map
                         :map m}))

(defmethod connection/handle-response :request-map [message]
   ;; (drawing/draw-terrain board-context (get message :map-data))
  (drawing/image-from-base64 (:map-data message) #(reset! terrain-image %)))

(defmethod connection/handle-response :unit-data [data]
  (logging/log "got " (count (:units data)) " units")
  (reset! game-units (:units data)))

;;; Concrete drawing functions

(defn draw-game [graphics]
  (when (and @running-game @game-units @terrain-image)
    (drawing/clear graphics)
    ;; Draw the map
    (drawing/draw-terrain-image graphics @terrain-image)
    ;; Draw the units
    (doseq [[c u] @game-units]
      (drawing/draw-unit-at graphics u c))
    ;; Draw the movement-range
    (doseq [c movement-range]
      (drawing/highlight-square graphics c :color "rgba(255, 0, 0, 0.4)"))

    (let [canvas (:canvas graphics)]
     (drawing/add-click-listener graphics
                                 [0 0] (.width canvas) (.height canvas)
                                 #(-> % drawing/canvas->map clicked-on)))))
