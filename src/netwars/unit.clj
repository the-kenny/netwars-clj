(ns netwars.unit
  (:use [netwars.unit-loader :as loader]))

(defn create-unit
  "Creates a unit with a specific color."
  ([type color]
     (assoc (get @loader/*unit-prototypes* type)
       :color color))
  ([type color & others]
     (apply assoc (create-unit type color) others)))
