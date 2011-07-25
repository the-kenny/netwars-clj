(ns netwars.drawing)

(defn make-graphics [element]
  {:context (.getContext element "2d")
   :canvas  element})

(defn resize-graphics [graphics w h]
  (let [canv (:canvas graphics)]
    (set! (. canv width) w)
    (set! (. canv height) h)))

(defn clear [graphics]
  (.clear (:context graphics)))

(defn draw-terrain [graphics image-data]
  (let [image (js/Image.)
        canvas (:canvas graphics)]
    (set! (. image src) image-data)
    (resize-graphics graphics (.width image) (.height image))
    (.drawImage (:context graphics) image
                (/ (- (.width canvas) (.width image)) 2)
                (/ (- (.height canvas) (.height image)) 2))))

(defn draw-image-at [graphics x y data]
  (let [image (js/Image.)
        canvas (:canvas graphics)]
    (set! (. image src) data)
    (.drawImage (:context graphics) image
                (/ (- (.width canvas) (.width image)) 2)
                (/ (- (.height canvas) (.height image)) 2))))
