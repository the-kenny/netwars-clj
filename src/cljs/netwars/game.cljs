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

(defn clicked-on [[x y]]
  (logging/log "clicked on: " x "/" y))

(defn unit-clicked [[x y] unit]
  (logging/message "Unit: " (name (:internal-name unit)) " (" (name (:color unit)) ") "
                    (:hp unit) "hp"))

(defmethod connection/handle-response :game-data [server message]
  (reset! running-game {:game-id (:game-id message)
                        :info (:info message)}))

(defn join-game [server game-id]
  (connection/send-data server {:type :join-game
                                :game-id game-id}))


;;; New game fns

(defn start-new-game [server map-name]
  (connection/send-data server {:type :new-game,
                                :map-name map-name}))

(defmethod connection/handle-response :new-game [server message]
  (logging//log "New game created!")
  ;; (request-game-data server (:game-id message))
  )


;;; Functions for handling data sent after a :game-data request

(defn request-map-data [server m]
  (connection/send-data server {:type :request-map
                                :map m}))

(defmethod connection/handle-response :request-map [_ message]
   ;; (drawing/draw-terrain board-context (get message :map-data))
  (drawing/image-from-base64 (:map-data message) #(reset! terrain-image %)))

(defmethod connection/handle-response :unit-data [_ data]
  (logging/log "got " (count (:units data)) " units")
  (reset! game-units (:units data)))

(defn draw-game [graphics]
  (when (and @running-game @game-units @terrain-image)
    (drawing/clear graphics)
    ;; Draw the map
    (drawing/draw-terrain-image graphics @terrain-image)
    ;; Draw the units
    (doseq [[c u] @game-units]
      (drawing/draw-unit-at graphics u c))

    (let [canvas (:canvas graphics)]
     ;; (drawing/add-event-listener graphics 0 0 (.width canvas) (.height canvas)
     ;;                             "onmouseover"
     ;;                             #(logging/log "moved: " %1 "," %2))
     (drawing/add-click-listener graphics
                                 [0 0] (.width canvas) (.height canvas)
                                 #(-> % drawing/canvas->map clicked-on)))))
