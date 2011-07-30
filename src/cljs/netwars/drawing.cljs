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

;; (defn request-image [server key & [callback]]
;;   (.send socket (js/JSON/stringify {:type :request-image
;;                                     :image key
;;                                     :request-id })))

;;; Canvas functions

(defn make-graphics [element]
  {:context (.getContext element "2d")
   :canvas  element})

(defn resize-graphics [graphics w h]
  (let [canv (:canvas graphics)]
    (set! (. canv width) w)
    (set! (. canv height) h)))

(defn clear [graphics]
  (.clear (:context graphics)))

;;; Netwars specific drawing functions

(defn draw-terrain-image [graphics image]
  (let [canvas (:canvas graphics)]
   (when (and (pos? (.width image)) (pos? (.height image)))
     (resize-graphics graphics (.width image) (.height image)))
   (.drawImage (:context graphics) image
               (/ (- (.width canvas) (.width image)) 2)
               (/ (- (.height canvas) (.height image)) 2))))

(defn draw-terrain [graphics image-data]
  (let [image (js/Image.)]
    (set! (. image src) image-data)
    (events/listen image "load" #(draw-terrain-image graphics image))))


;;; Tile handling

(defn request-unit-tiles [server]
  (connection/send-data server {:type :unit-tiles}))

(def unit-tiles (atom nil))

(defmethod connection/handle-response :unit-tiles [server response]
  (connection/log "got tiles!")
  (let [image (js/Image.)]
    (set! (. image src) (:tiled-image response))
    (events/listen image "load"
                   (fn []
                      (reset! unit-tiles {:tile-spec (:tile-spec response)
                                         :tiled-image image})
                      (connection/log "Finished loading tiled image"))))
  ;; (doseq [k (keys (:tile-spec @unit-tiles))]
  ;;   (connection/log (name (first k)) " " (name (second k))))
  )

(let [color-mappings {:yellow :ys
                      :red :os
                      :green :ge
                      :blue :bm
                      :black :bh}]
 (defn draw-unit-at [graphics x y unit]
   (let [internal-name (:internal-name unit)
         color (:color unit)
         canvas (:canvas graphics)
         tile (:tiled-image @unit-tiles)
         context (:context graphics)
         [tx ty] (get (:tile-spec @unit-tiles)
                         [(color-mappings color) internal-name])]
     (. context (drawImage tile
                           tx ty
                           16 16
                           (* x 16)
                           (* y 16)
                           16 16)))))
