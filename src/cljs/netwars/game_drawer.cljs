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
  (coord (Math/floor (/ (:x c)
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

(defn- load-terrain
  ([map-name callback]
     (let [image (js/Image.)]
       (set! (.-onload image) #(callback image))
       (set! (.-src image) (str "api/render-map/" map-name))
       image)))
;; (def load-terrain (memoize load-terrain*))

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

(defn test-drawing [canvas game]
  (prepare-canvas canvas game
                  (fn [canvas game] (draw-terrain canvas game
                                                  #(logging/log "Drawing finished!")))))
