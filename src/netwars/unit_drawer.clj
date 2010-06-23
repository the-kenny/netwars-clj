(ns netwars.unit-drawer
  (:use [netwars.unit :as unit]
        [netwars.sprite-loader :as sprites])
  (:import java.awt.Graphics2D
           java.awt.image.BufferedImage))

(def ^{:private true} +number-mappings+
     (zipmap (iterate inc 1) ["one" "two" "three"
                              "four" "five" "six"
                              "seven" "eight" "nine"]))

(defn- hp-pixmap [hp]
  (when (< 0 hp 10)
   (sprites/load-pixmap
    (str "pixmaps/units/misc/" (get +number-mappings+ hp) ".png"))))

(def ^{:private true
       :doc "A map. Key corresponds to the keys in an unit-struct, v is a
 function which returns a java.awt.BufferedImage or nil."}
     +unit-draw-properties+
     {:hp hp-pixmap})

(defn draw-unit-to-image [unit]
  (let [image (BufferedImage. 16 16 BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)]
    (.drawImage graphics (load-unit-tile (:internal-name unit)
                                         (:color unit)) 0 0 nil)
    (doseq [[k v] unit]
            (when-let [img ((get +unit-draw-properties+ k
                                 (fn [& _] nil)) v)]
              (.drawImage graphics img 0 0 nil)))
    (.finalize graphics)
    image))
