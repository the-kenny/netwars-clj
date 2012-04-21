(ns netwars.tile-drawer
  (:require [netwars.tiles :as tiles]
            [netwars.logging :as logging]
            [clojure.browser.dom :as dom]))


(defn- load-tile-image*
  ([file callback]
     (let [image (js/Image.)]
       (set! (.-src image) file)
       (set! (.-onload image) callback)
       image))
  ([file]
     (load-tile-image* file #(logging/log "image loaded:" file))))
(def load-tile-image (comp (memoize load-tile-image*) tiles/tile-filename))

(def +terrain-tiles-image+ (load-tile-image tiles/+terrain-tiles+))
(def +unit-tiles-image+ (load-tile-image tiles/+unit-tiles+))
(def +unit-meta-tiles-image+ (load-tile-image tiles/+unit-meta-tiles+))

(defn draw-tile [context tile tile-path [dx dy]]
  (if-let [rect (tiles/tile-rect tile tile-path)]
    (let [{sx :x, sy :y, width :width, height :height} rect
          image (load-tile-image tile)]
      (.drawImage context
                  image
                  sx sy
                  width height
                  dx dy
                  width height))
    (logging/log "Tile not found:" tile-path)))
