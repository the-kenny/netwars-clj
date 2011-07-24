(ns netwars.drawing)

(defn make-graphics [element]
  {:context (.getContext element "2d")
   :canvas  element})

(defn clear [graphics]
  (.clear (:context graphics)))

(defn draw-terrain [graphics image-data]
  (let [image (js/Image.)
        canvas (:canvas graphics)]
    (set! (. image src) image-data)
    (.drawImage (:context graphics) image
                (/ (- (.width canvas) (.width image)) 2)
                (/ (- (.height canvas) (.height image)) 2))))
