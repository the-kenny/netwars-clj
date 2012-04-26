(ns netwars.game-drawer
  (:require [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.game-board :as game-board]
            [netwars.tile_drawer :as tile-drawer]
            [netwars.tiles :as tiles]
            [netwars.logging :as logging]
            [netwars.map-renderer :as map-renderer]
            [netwars.map-utils :as map-utils]
            [clojure.browser.dom :as dom]))

(def +field-width+  16)
(def +field-height+ 16)

(defn canvas->coord
  "Converts canvas coordinates to netwars coordinates"
  [[x y]]
  (aw-map/coord (Math/floor (/ x
                               +field-width+))
                (Math/floor (/ y
                               +field-height+))))

(defn coord->canvas
  "Converts netwars coordinates to canvas coordinates"
  ([[x y] center?]
     (let [x x
           y y
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
                           cc
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
    (doseq [x (range (aw-map/width terrain-board))
            y (range (aw-map/height terrain-board))
            :let [c (aw-map/coord x y)]]
      (render-background-for-coordinate context terrain-board c nil))
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
                           cc
                           nil))
  (when (some aw-unit/low-ammo? (vals (aw-unit/available-weapons unit)))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [:ammo]
                           [+field-width+ +field-width+]
                           cc
                           nil))
  (when (and (aw-unit/can-transport? unit)
             (not (empty? (-> unit :transport :freight))))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [:loaded]
                           [+field-width+ +field-width+]
                           cc
                           nil))
  ;; TODO: fuel, hidden
  (when (and (aw-map/is-building? terrain)
             (not= (aw-map/capture-points terrain)
                   aw-map/+building-capture-points+))
    (tile-drawer/draw-tile context
                           tiles/+unit-meta-tiles+
                           [:minus]
                           [+field-width+ +field-height+]
                           cc
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
                     (let [unit-canvas (dom/element :canvas)
                           unit-context (.getContext unit-canvas "2d")
                           unit-tile-area (tiles/tile-rect tiles/+unit-tiles+ path)
                           [cx cy] cc]
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
                       (.drawImage context unit-canvas cx cy))))]
    (tile-drawer/draw-tile context
                           tiles/+unit-tiles+
                           path
                           [+field-width+ +field-width+]
                           cc
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
               (.drawImage (.getContext canvas "2d") @hidden-background-canvas 0 0)
               (callback canvas game))]

    (cond
     (nil? @hidden-background-canvas)
     (let [newcanvas (dom/element :canvas)]
       (prepare-canvas newcanvas game
                       (fn [newcanvas game]
                         (draw-map-background (.getContext newcanvas "2d")
                                              game
                                              (fn []
                                                (reset! hidden-background-canvas newcanvas)
                                                (cont canvas game))))))
     last-clicked-coord
     (do
       (logging/log "Redrawing on hidden canvas")
       (render-background-for-coordinate (.getContext @hidden-background-canvas "2d")
                                        (-> game :board :terrain)
                                        last-clicked-coord
                                        (fn []
                                          (cont canvas game))))

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
          selected-coord (aw-game/selected-coordinate game)]
      (highlight-squares context
                         (disj (aw-game/movement-range game) selected-coord)
                         "rgba(255, 0, 0, 0.4)")
      (highlight-squares context [selected-coord] "rgba(0, 0, 0, 0.3)")))
  (when (fn? callback) (callback canvas game)))

(defn draw-attackable-units [canvas game callback]
  (when-let [selection (aw-game/selected-coordinate game)]
    (let [targets (aw-game/attackable-targets game)]
      (highlight-squares (.getContext canvas "2d")
                         targets
                         "rgba(50,50,50,0.6)")))
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
                             (draw-attackable-units
                              canvas game
                              #(logging/log "Drawing finished!")))))))
                     last-clicked-coord))))
