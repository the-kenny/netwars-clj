(ns netwars.game-drawer
  (:require [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.game-board :as game-board]
            [netwars.tile_drawer :as tile-drawer]
            [netwars.tiles :as tiles]
            [netwars.logging :as logging]
            [clojure.browser.dom :as dom]))

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

(defn- draw-unit [context c unit]
  ;; TODO: Draw unit meta information
  (let [cc (coord->canvas c)]           ;canvas coordinate
   (tile-drawer/draw-tile context
                          tiles/+unit-tiles+
                          [(:color unit) (:internal-name unit)]
                          [(:x cc) (:y cc)]
                          (fn [] nil))))

(defn- highlight-squares [context cs color]
  (.save context)
  (set! (.-fillStyle context) color)
  (doseq [c cs
          :let [{:keys [x y]} (coord->canvas c)]]
    (.fillRect context x y +field-width+ +field-height+))
  (.restore context))

(defn- prepare-canvas [canvas game callback]
  (let [width (aw-map/width (-> game :board :terrain))
        height (aw-map/height (-> game :board :terrain))]
    (set! (.-width canvas) (* width +field-width+))
    (set! (.-height canvas) (* height +field-height+))
    (callback canvas game)))

(defn- draw-terrain [canvas game callback]
  (load-terrain (-> game :info :map-name)
                (fn [image]
                  (.drawImage (.getContext canvas "2d")
                              image
                              0 0)
                  (callback canvas game))))

(defn- draw-units [canvas game callback]
  (let [context (.getContext canvas "2d")]
    (doseq [[c u] (-> game :board :units)]
      (draw-unit context c u)))
  (callback canvas game))

(defn draw-selected-unit [canvas game callback]
  (when-let [unit (aw-game/selected-unit game)]
    (let [context (.getContext canvas "2d")
          selected-coord (aw-game/selected-coordinate game)]
      (highlight-squares context
                         (disj (aw-game/movement-range game) selected-coord)
                         "rgba(255, 0, 0, 0.4)")
      (highlight-squares context [selected-coord] "rgba(0, 0, 0, 0.3)")))
  (callback canvas game))

(defn draw-game [canvas game]
  (prepare-canvas canvas game
                  (fn [canvas game]
                    (draw-terrain canvas game
                                  (fn [canvas game]
                                    (draw-units canvas game
                                                (fn [canvas game]
                                                  (draw-selected-unit canvas game
                                                                      #(logging/log "Drawing finished!")))))))))
