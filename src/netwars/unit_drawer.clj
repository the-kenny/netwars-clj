(ns netwars.unit-drawer
  (:use [netwars.unit :as unit]
        [netwars.sprite-loader :as sprites])
  (:import java.awt.Graphics2D
           java.awt.image.BufferedImage))

(defmulti ^{:private true} property-pixmap key)

(def ^{:private true} +number-mappings+
     (zipmap (iterate inc 1) ["one" "two" "three"
                              "four" "five" "six"
                              "seven" "eight" "nine"]))

(defmethod property-pixmap :hp [[_ hp]]
           (when (< 0 hp 10)
             (sprites/load-pixmap
              (str "pixmaps/units/misc/" (get +number-mappings+ hp) ".png"))))

(defmethod property-pixmap :default [_]
           nil)

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
    (doseq [entry unit]
            (when-let [img (property-pixmap entry)]
              (.drawImage graphics img 0 0 nil)))
    (.finalize graphics)
    image))
