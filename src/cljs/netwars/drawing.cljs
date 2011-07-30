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


(defn draw-unit-at [graphics x y data]
  (let [image (js/Image.)
        canvas (:canvas graphics)]
    (set! (. image src) data)
    (.drawImage (:context graphics) image
                (/ (- (.width canvas) (.width image)) 2)
                (/ (- (.height canvas) (.height image)) 2))))

;;; Tile handling

(defn request-unit-tiles [server]
  (connection/send-data server {:type :unit-tiles}))

(def unit-tiles (atom nil))
(defmethod connection/handle-response :unit-tiles [server response]
  (connection/log "got tiles!")
  (reset! unit-tiles (select-keys response [:tile-spec :tiled-image])))
