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
  "Loads the pixmap of an unit defined as [unit-keyword color-keyword]"
  [[unit color]]
  (load-pixmap (str "pixmaps/units/"
                    (get +unit-color-mappings+ color)
                    "/"
                    (name unit) ".png")))

(defn load-terrain-tile [[terrain direction]]
  (load-pixmap (str "pixmaps/ground/"
                    (name terrain)
                    "/"
                    (name direction)
                    ".png")))
