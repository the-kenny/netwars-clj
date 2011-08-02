(ns netwars.drawing
  (:require [netwars.connection :as connection]
            [goog.events :as events]))

;;; Image loading and caching

(let [cache (atom {})]
 (defn cache-image! [key image-data]
   (let [image (js/Image.)]
     (set! (. image src) image-data)
    (swap! cache assoc key image)))

 (defn get-cached-image [key]
   (when-let [img (get cache key)]
     img)))


;;; Canvas functions

(defn make-graphics [element]
  (let [kinetic (js/Kinetic. (.id element) "2d")]
   {:kinetic kinetic
    :context (. kinetic (getContext))
    :canvas  (. kinetic (getCanvas))}))

(defn resize-graphics [graphics w h]
  (let [canv (:canvas graphics)]
    (set! (. canv width) w)
    (set! (. canv height) h)))

(defn redraw [graphics]
  (. (:kinetic graphics) (drawStage)))

(defn clear [graphics]
  (. (:kinetic graphics) (clear)))

(defn set-drawing-function! [graphics f]
  (. (:kinetic graphics) (setDrawStage #(f graphics))))

(defn start-animation [graphics]
  (. (:kinetic graphics) (startAnimation)))

(defn image-from-base64 [base64 callback]
  (let [image (js/Image.)]
    (set! (. image src) base64)
    (events/listen image "load" #(callback image))))

;;; Netwars specific drawing functions

(defn draw-terrain-image [graphics image]
  (let [canvas (:canvas graphics)]
   (when (and (pos? (.width image)) (pos? (.height image)))
     (resize-graphics graphics (.width image) (.height image)))
   (.drawImage (:context graphics) image
               (/ (- (.width canvas) (.width image)) 2)
               (/ (- (.height canvas) (.height image)) 2))))

(defn draw-terrain [graphics image-data]
  (image-from-base64 image-data (partial draw-terrain-image graphics)))


;;; Tile handling

(defn request-unit-tiles [server]
  (connection/send-data server {:type :unit-tiles}))

(def unit-tiles (atom nil))

(defmethod connection/handle-response :unit-tiles [server response]
  (connection/log "got tiled unit-sprites")
  (image-from-base64 (:tiled-image response)
                     (fn [img]
                       (reset! unit-tiles {:tile-spec (:tile-spec response)
                                           :tiled-image img})
                       (connection/log "Finished loading tiled image"))))

(let [color-mappings {:yellow :ys
                      :red :os
                      :green :ge
                      :blue :bm
                      :black :bh}]
 (defn draw-unit-at [graphics unit x y & [callback]]
   (let [internal-name (:internal-name unit)
         color (:color unit)
         canvas (:canvas graphics)
         tile (:tiled-image @unit-tiles)
         context (:context graphics)
         kinetic (:kinetic graphics)
         [tx ty] (get (:tile-spec @unit-tiles)
                      [(color-mappings color) internal-name])]
     ;; (. kinetic (clear))
     (. kinetic (beginRegion))
     (. context (drawImage tile
                           tx ty
                           16 16
                           (* x 16)
                           (* y 16)
                           16 16))
     (. context (beginPath))
     (. context (rect (* x 16) (* y 16) 16 16))
     (. context (closePath))
     (when (fn? callback)
      (. kinetic (addRegionEventListener "onmousedown"
                                         #(callback x y unit))))
     (. kinetic (closeRegion)))))
