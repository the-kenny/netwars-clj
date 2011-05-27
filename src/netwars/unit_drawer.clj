(ns netwars.unit-drawer
  (:use [netwars.utilities :as util]
        [netwars.aw-unit :as unit])
  (:import java.awt.Graphics2D
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))


(let [tile-cache (atom {})]
  (defn- load-pixmap [#^String file]
    (if-let [img (get @tile-cache file)]
      img
      (if-let [res (util/load-resource file)]
        (get (swap! tile-cache assoc file (ImageIO/read res))
             file)))))


(let [unit-color-mappings {:red     "os"
                           :blue    "bm"
                           :green   "ge"
                           :yellow  "yc"
                           :black   "bm"}] 
  (defn- load-unit-tile
    "Loads the pixmap of an unit given at defined as [unit color] and caches it."
    [unit color]
    (load-pixmap (str "pixmaps/units/"
                      (get unit-color-mappings color)
                      "/"
                      (name unit) ".png"))))


(defmulti property-pixmap (fn [x kv] (key kv)))


(let [number-filenames  ["one" "two" "three"
                         "four" "five" "six"
                         "seven" "eight" "nine"]]
  (defmethod property-pixmap :hp [unit [_ hp]]
    (when (< 0 hp 10)
      (load-pixmap
       (str "pixmaps/units/misc/" (get number-filenames (dec hp)) ".png")))))

(defmethod property-pixmap :fuel [unit [_ fuel]]
  (when (< (/ fuel (:max-fuel-level (meta unit))) 0.5)
     (load-pixmap "pixmaps/units/misc/fuel.png")))

(defmethod property-pixmap :default [_ _]
  nil)


(defn draw-unit [graphics [unit]]
  (.drawImage graphics (load-unit-tile (:internal-name unit)
                                       (:color unit))
              0 0 nil)
  (doseq [entry unit]
    (when-let [img (property-pixmap unit entry)]
      (.drawImage graphics img 0 0 nil))))

(defn draw-unit-to-image [unit]
  (let [image (BufferedImage. 16 16 BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)]
    (.drawImage graphics (load-unit-tile (:internal-name unit)
                                         (:color unit)) 0 0 nil)
    (doseq [entry unit]
      (when-let [img (property-pixmap unit entry)]
        (.drawImage graphics img 0 0 nil)))
    (.finalize graphics)
    image))
