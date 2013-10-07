(ns netwars.game-drawer
  (:require [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.game-board :as game-board]
            [netwars.pathfinding :as pathfinding]
            [netwars.tile_drawer :as tile-drawer]
            [netwars.tiles :as tiles]
            [netwars.logging :as logging]
            [netwars.map-renderer :as map-renderer]
            [netwars.map-utils :as map-utils]
            [goog.dom :as gdom]))

(def +field-width+  16)
(def +field-height+ 16)

(defn canvas->coord
  "Converts canvas coordinates to netwars coordinates"
  [c]
  (aw-map/coord (Math/floor (/ (:x c)
                               +field-width+))
                (Math/floor (/ (:y c)
                               +field-height+))))

(defn coord->canvas
  "Converts netwars coordinates to canvas coordinates"
  ([c center?]
     (let [x (:x c)
           y (:y c)
           xn (* x +field-width+)
           yn (* y +field-height+)]
       (apply aw-map/coord
              (if center?
                [(+ xn (/ +field-width+ 2)) (+ yn (/ +field-height+ 2))]
                [xn yn]))))
  ([c] (coord->canvas c false)))

(let [cache (atom {})]
 (defn- load-terrain
   ([map-name callback]
      (if-let [cached (get @cache map-name)]
        (callback cached)
        (let [image (js/Image.)]
          (set! (.-onload image) #(do
                                    (swap! cache assoc map-name image)
                                    (callback image)))
          (set! (.-src image) (str "api/render-map/" map-name))
          nil)))))
;; (def load-terrain (memoize load-terrain*))

;;; TODO: Callback?
(defn- drawing-fn [context c path]
  (let [cc (coord->canvas c)]
    (tile-drawer/draw-tile context
                           tiles/+terrain-tiles+
                           path
                           [+field-width+ +field-width+]
                           [(:x cc) (:y cc)]
                           ;; TODO: Render the next tile using this callback
                           nil)))

(defn render-background-for-coordinate [context terrain-board c callback]
  (when-let [terr (aw-map/at terrain-board c)]
    (map-renderer/draw-tile (if (aw-map/is-building? terr)
                              (aw-map/reset-capture-points terr)
                              terr)
                            (map-utils/neighbours terrain-board c)
                            #(drawing-fn context c %))
    (when (fn? callback) (callback))))

(defn- draw-map-background [context game callback]
  (let [terrain-board (-> game :board :terrain)]
    (dotimes [x (aw-map/width terrain-board)]
      (dotimes [y (aw-map/height terrain-board)]
        (render-background-for-coordinate context
                                          terrain-board
                                          (aw-map/coord x y)
                                          nil)))
    (when (fn? callback) (callback))))

;;; General functions for drawing units, background etc.

(defn- draw-unit-meta [context cc unit terrain]
  (assert (meta unit))
  (when (< (:hp unit) (-> unit meta :hp))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [(nth [:one :two :three :four :five
                                  :six :seven :eight :nine]
                                 (dec (:hp unit)))]
                           [+field-width+ +field-width+]
                           [(:x cc) (:y cc)]
                           nil))
  (when (some aw-unit/low-ammo? (vals (aw-unit/available-weapons unit)))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [:ammo]
                           [+field-width+ +field-width+]
                           [(:x cc) (:y cc)]
                           nil))
  (when (and (aw-unit/can-transport? unit)
             (not (empty? (-> unit :transport :freight))))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [:loaded]
                           [+field-width+ +field-width+]
                           [(:x cc) (:y cc)]
                           nil))
  ;; TODO: fuel, hidden
  (when (and (aw-map/is-building? terrain)
             (not= (aw-map/capture-points terrain)
                   aw-map/+building-capture-points+))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [:minus]
                           [+field-width+ +field-height+]
                           [(:x cc) (:y cc)]
                           nil)))

(defn- highlight-squares [context cs color]
  (.save context)
  (set! (.-fillStyle context) color)
  (doseq [c cs
          :let [{:keys [x y]} (coord->canvas c)]]
    (.fillRect context x y +field-width+ +field-height+))
  (.restore context))

(defn- draw-unit [context game c unit]
  (let [cc (coord->canvas c)            ;canvas coordinate
        path [(:color unit) (:internal-name unit)]
        terrain (game-board/get-terrain (:board game) c)
        cont (fn []
               (when (:moved unit)
                     (let [unit-canvas (gdom/createElement "canvas")
                           unit-context (.getContext unit-canvas "2d")
                           unit-tile-area (tiles/tile-rect tiles/+unit-tiles+ path)]
                       (set! (.-width unit-canvas) (:width unit-tile-area))
                       (set! (.-height unit-canvas) (:width unit-tile-area))
                       (tile-drawer/draw-tile unit-context
                                              tiles/+unit-tiles+
                                              path
                                              [+field-width+ +field-width+]
                                              [0 0]
                                              nil)
                       (set! (.-globalCompositeOperation unit-context) "source-in")
                       (highlight-squares unit-context [(aw-map/coord 0 0)] "rgba(0,0,0,0.4)")
                       (.drawImage context unit-canvas (:x cc) (:y cc)))))]
    (tile-drawer/draw-tile context
                           tiles/+unit-tiles+
                           path
                           [+field-width+ +field-width+]
                           [(:x cc) (:y cc)]
                           (fn [] (cont) (draw-unit-meta context cc unit terrain)))))

(defn- prepare-canvas [canvas game callback]
  (let [width (aw-map/width (-> game :board :terrain))
        height (aw-map/height (-> game :board :terrain))]
    (set! (.-width canvas) (* width +field-width+))
    (set! (.-height canvas) (* height +field-height+))
    (when (fn? callback) (callback canvas game))))

;;; Functions to render maps

;;; Let's see:
;;;
;;; The rendering of the background isn't fast enouth. Let's do the
;;; following: Initially render the map to a hidden canvas and render
;;; it from there to the game canvas. Every time the user does
;;; something (e.g. captures a building) this single coordinate is
;;; re-rendered on the hidden canvas and the result is blitted on the
;;; game canvas.

(def hidden-background-canvas (atom nil))

(defn- draw-terrain [canvas game callback & [last-clicked-coord]]
  (let [cont (fn [canvas game]
               (.drawImage (.getContext canvas "2d") (second @hidden-background-canvas) 0 0)
               (callback canvas game))
        [map-url hidden-canvas] @hidden-background-canvas]

    (cond
     (or (nil? hidden-canvas)
         (not= map-url (:map-url game)))
     (let [newcanvas (gdom/createElement "canvas")]
       (prepare-canvas newcanvas game
                       (fn [newcanvas game]
                         (draw-map-background (.getContext newcanvas "2d")
                                              game
                                              (fn []
                                                (reset! hidden-background-canvas [(:map-url game)
                                                                                  newcanvas])
                                                (cont canvas game))))))

     last-clicked-coord
     (render-background-for-coordinate (.getContext hidden-canvas "2d")
                                       (-> game :board :terrain)
                                       last-clicked-coord
                                       #(cont canvas game))

     true
     (cont canvas game))))

(defn- draw-units [canvas game callback]
  (let [context (.getContext canvas "2d")]
    (doseq [[c u] (-> game :board :units)]
      (draw-unit context game c u)))
  (when (fn? callback) (callback canvas game)))

(defn draw-selected-unit [canvas game callback]
  (when-let [unit (aw-game/selected-unit game)]
    (let [context (.getContext canvas "2d")
          selected-coord (aw-game/selected-coordinate game)
          selected-unit  (aw-game/selected-unit game)]
      (when (and (not (:moved selected-unit))
                 ;; Check for the current path because the movement
                 ;; range should just get drawn when the unit can
                 ;; move. When the player clicks on 'Attack' the unit
                 ;; can't move anymore, therefore we dissoc the
                 ;; :current-path from the selected unit.
                 (:current-path game))
       (highlight-squares context
                          (disj (aw-game/movement-range game) selected-coord)
                          "rgba(255, 0, 0, 0.4)"))
      (highlight-squares context [selected-coord] "rgba(0, 0, 0, 0.3)")))
  (when (fn? callback) (callback canvas game)))

(defn draw-attackable-units [canvas game callback]
  (when-let [selection (aw-game/selected-coordinate game)]
    (let [targets (aw-game/attackable-targets game)]
      (highlight-squares (.getContext canvas "2d")
                         targets
                         "rgba(255,0,0,0.8)")))
  (when (fn? callback) (callback canvas game)))

(defn draw-path [canvas game callback]
  (when-let [path (:current-path game)]
    (let [context (.getContext canvas "2d")
          canvas-path (map #(coord->canvas % true) (pathfinding/elements path))]
      (.beginPath context)
      (set! (.-strokeStyle context) "rgba(0,0,0,0.5)")
      (set! (.-lineWidth context) 4)
      (set! (.-lineCap  context) "round")
      (set! (.-lineJoin context) "round")
      (let [c (first canvas-path)]
        (.moveTo context (:x c) (:y c)))
      (doseq [c (rest canvas-path)]
        (.lineTo context (:x c) (:y c)))
      (.stroke context)
      (.beginPath context)
      (let [end (last canvas-path)]
        (.arc context
              (:x end) (:y end)
              2.0
              0
              (* 2 Math/PI)))
      (.stroke context)))
  (when (fn? callback) (callback canvas game)))

(defn draw-game [canvas game & [last-clicked-coord]]
  (prepare-canvas canvas game
                  (fn [canvas game]
                    (draw-terrain
                     canvas game
                     (fn [canvas game]
                       (draw-units
                        canvas game
                        (fn [canvas game]
                          (draw-selected-unit
                           canvas game
                           (fn [canvas game]
                             (draw-path
                              canvas game
                              (fn [canvas game]
                                (draw-attackable-units
                                 canvas game
                                 #(logging/log "Drawing finished!")))))))))
                     last-clicked-coord))))
