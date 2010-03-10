(ns netwars.sprite-loader
  (:use [clojure.contrib.duck-streams :only [reader]]
        netwars.utilities)
  (:import javax.imageio.ImageIO))

(def pixmap-cache (atom {}))

(defn load-pixmap [#^String file]
  (if-let [img (get @pixmap-cache file)]
    img
    (if-let [res (load-resource file)]
      (get (swap! pixmap-cache assoc file
                (ImageIO/read res))
         file))))

(def +unit-color-mappings+
     {:red     "os"
      :blue    "bm"
      :green   "ge"
      :yellow  "yc"
      :black   "bm"})

(defn load-unit-tile
  "Loads the pixmap of an unit given at defined as [unit color] and caches it."
  [[unit color]]
  (load-pixmap (str "pixmaps/units/"
                    (get +unit-color-mappings+ color)
                    "/"
                    (name unit) ".png")))


;; (defn load-terrain-tile
;;   "Loads a terrain-tile given as [terrain direction], terrain and directions
;;   being keywords.
;;   Loads the specific sprite from 'pixmaps/ground/$terrain/$direction.png'
;;   If direction is nil, loads the sprite 'pixmaps/ground/$terrain.png'
;;   The sprite will be cached."
;;   [[terrain direction]]
;;   (load-pixmap (str "pixmaps/ground/"
;;                     (name terrain)
;;                     (when direction
;;                       (str "/" (name direction)))
;;                     ".png")))

(defn load-terrain-tile
  "Loads a terrain tile from pixmaps/grounds.
  Every segment is a keyword describing a level in the filesystem structure:
  street/lr.png would be [:street :lr]
  If a segment is nil, it will be ignored."
  [[& segments]]
  (load-pixmap (str (reduce #(when %2 (str %1 "/" (name %2)))
                            "pixmaps/ground"
                            (remove nil? segments))
                    ".png")))

(defn load-building-tile [[building color]]
  (load-pixmap (str "pixmaps/ground/buildings/"
                    (name building)
                    "/"
                    (name color)
                    ".png")))
