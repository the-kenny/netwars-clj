(ns netwars.map-drawer
  (:use netwars.map-renderer
        netwars.aw-map
        netwars.map-utils)
  (:require [netwars.tiles :as tiles]
            [clojure.java.io :as io])
  (:import java.awt.Graphics2D
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))

(let [tile-cache (atom {})]
  (defn- load-pixmap [#^String file]
    (if-let [img (get @tile-cache file)]
      img
      (if-let [res (io/resource file)]
        (get (swap! tile-cache assoc file (ImageIO/read res))
             file)))))

(defn- terrain-tile-image []
  (->  tiles/+terrain-tiles+
       tiles/tile-filename
       io/resource
       ImageIO/read))
(alter-var-root #'terrain-tile-image memoize)

(defn drawing-fn [graphics c path]
  (if-let [area (tiles/tile-rect tiles/+terrain-tiles+
                                 path)]
    (let [{sx :x, sy :y, width :width, height :height} area
          [dx dy] c
          image (terrain-tile-image)]
      (.drawImage graphics
                  image
                  (* dx 16)
                  (- (* dy 16) (- (.getHeight image) 16))
                  (+ (* dx 16) width)
                  (- (+ (* dy 16) height) (- (.getHeight image) 16))
                  sx
                  sy
                  (+ sx width)
                  (+ sy height)
                  nil))
    (println path " not found.")))

(defn render-terrain-board [terrain-board]
  (let [image (BufferedImage. (* 16 (width terrain-board))
                              (* 16 (height terrain-board))
                              BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)]
    (.drawImage graphics (load-pixmap "background.png") 0 0 nil)
    (doseq [x (range (width terrain-board))
            y (range (height terrain-board))
            :let [c (coord x y)]]
      (when-let [terr (at terrain-board c)]
        (draw-tile terr (neighbours terrain-board c)
                   (partial drawing-fn graphics c))))
    (.finalize graphics)
    image))

(comment
  (require 'netwars.game-creator)
  (def testmap (netwars.game-creator/make-game {} "maps/7330.aws"))
  (javax.imageio.ImageIO/write (render-terrain-board (-> testmap :board :terrain))
                               "png"
                               (java.io.File. "/Users/moritz/testmap.png")))

;; (defn render-map-to-image [loaded-map]
;;   (let [terrain (:terrain loaded-map)
;;         image (BufferedImage. (* 16 (width terrain))
;;                               (* 16 (height terrain))
;;                               BufferedImage/TYPE_INT_ARGB)
;;         graphics (.createGraphics image)]
;;     (.drawImage graphics (load-pixmap "pixmaps/background.png") 0 0 nil)
;;     (doseq [x (range (width terrain))
;;             y (range (height terrain))
;;             c (coord x y)]
;;       (when-let [terr (at terrain c)]
;;         (draw-tile terr (neighbours loaded-map c)
;;                           (partial drawing-fn graphics c))))
;;     (.finalize graphics)
;;     image))

;; (defn render-path-to-map [map-img path]
;;   (let [lst (:path path)
;;         img (load-pixmap "pixmaps/misc/path.png")
;;         graphics (.createGraphics map-img)]
;;     (doseq [[x y _] lst]
;;       (println x y)
;;       (draw-img graphics c img)))
;;   map-img)
