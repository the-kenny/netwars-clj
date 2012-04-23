(ns netwars.tile-drawer
  (:require [netwars.tiles :as tiles]
            [netwars.logging :as logging]
            [clojure.browser.dom :as dom]))

(let [cache (atom {})]
  (defn- load-tile-image [tile callback]
    (let [file (tiles/tile-filename tile)]
     (if-let [cached (get @cache file)]
       (callback cached)
       (let [image (js/Image.)]
         (set! (.-onload image) #(callback image))
         (set! (.-src image) file)
         (swap! cache assoc file image)
         image)))))

(load-tile-image tiles/+terrain-tiles+   #(logging/log "tile loaded: terrain"))
(load-tile-image tiles/+unit-tiles+      #(logging/log "tile loaded: unit"))
(load-tile-image tiles/+unit-meta-tiles+ #(logging/log "tile loaded: unit-meta"))

(defn draw-tile [context
                 tile
                 tile-path
                 [field-width field-height]
                 [dx dy]
                 callback]
  (if-let [rect (tiles/tile-rect tile tile-path)]
    (let [{sx :x, sy :y, width :width, height :height} rect]
      (load-tile-image tile
                       (fn [image]
                         (.drawImage context
                                     image
                                     sx sy
                                     width height
                                     (- dx (- width field-width))
                                     (- dy (- height field-height))
                                     width height)
                         (when (fn? callback) (callback)))))
    (do (logging/log "Tile not found: " (if (sequential? tile-path)
                                          (apply str (map str tile-path))
                                          tile-path))
        (when (fn? callback) (callback)))))
