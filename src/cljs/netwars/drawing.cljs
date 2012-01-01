(ns netwars.drawing
  (:require [netwars.connection :as connection]
            [netwars.logging :as logging]
            [clojure.browser.event :as event]
            [netwars.pathfinding :as pathfinding]
            [kinetic :as kinetic]))

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

;;; Event Stuff

(defn add-event-listener [graphics [x y] w h event f]
  (let [kinetic (:kinetic graphics)
        context (:context graphics)
        callback #(let [mousePos (. kinetic (getMousePos))
                        x (.x mousePos)
                        y (.y mousePos)]
                    (f [x y]))]
    ;; Add region event listener
    (. kinetic (beginRegion))
    (. context (beginPath))
    (. context (rect x y w h))
    (. context (closePath))
    (. kinetic (addRegionEventListener event (fn [e]
                                                (callback)
                                                (. (.event js/window)
                                                   (preventDefault)))))
    (. kinetic (closeRegion))))

(defn add-click-listener
  "Adds a click listener to graphics at [x,y] with width w and height h.
   Calls (f mouse-x mouse-y) on click"
  [graphics [x y] w h f]
  (add-event-listener graphics [x y] w h "onmousedown" f))

(defn add-move-listener
  "Adds a move listener to the canvas contained in the graphics object.
   Calls (f [x y])"
  [graphics f]
  (let [kinetic (:kinetic graphics)
        canvas (:canvas graphics)
        last-coordinate (atom nil)
        last-atom (atom nil)]
    (.addEventListener canvas "mousemove"
                       (fn [event]
                         (let [mouse-pos (. kinetic (getMousePos))
                               c (canvas->map [(.x mouse-pos) (.y mouse-pos)])]
                           (when (not= c @last-atom)
                             (f c)
                             (reset! last-atom c)))))))

;;; Helper

(defn image-from-base64 [base64 callback]
  (let [image (js/Image.)]
    (set! (. image src) base64)
    (event/listen-once image :load (fn [_] (callback image)))))

(defn canvas->map
  "Converts canvas coordinates to netwars coordinates"
  [[x y]]
  [(Math/floor (/ x 16)) (Math/floor (/ y 16))])

(defn map->canvas
  "Converts canvas coordinates to netwars coordinates"
  [[x y] & center?]
  (if center?
    [(+ (* x 16) 8) (+ (* y 16) 8)]
    [(* x 16) (* y 16)]))

;;; Terrain drawing

(defn draw-terrain-image [graphics image]
  (let [canvas (:canvas graphics)]
   (when (and (pos? (.width image)) (pos? (.height image)))
     (resize-graphics graphics (.width image) (.height image)))
   (.drawImage (:context graphics) image
               (/ (- (.width canvas) (.width image)) 2)
               (/ (- (.height canvas) (.height image)) 2))))

(defn draw-terrain [graphics image-data]
  (image-from-base64 image-data (partial draw-terrain-image graphics)))


(defn highlight-square [graphics [x y] & {:keys [color]}]
  (let [context (:context graphics)]
    (when color
     (set! (. context fillStyle) color))
    (.fillRect context (* x 16) (* y 16) 16 16)))

;;; Tile handling

(def unit-tiles (atom nil))

(defn load-unit-tiles
  "Requests unit tiles from server. No-op if tiles are already loaded."
  []
  (when (nil? @unit-tiles)
    ;; Request unit tiles
    (connection/send-data {:type :unit-tiles})))

(defmethod connection/handle-response :unit-tiles [response]
  (logging/log "got tiled unit-sprites")
  (image-from-base64 (:tiled-image response)
                     (fn [img]
                       (reset! unit-tiles {:tile-spec (:tile-spec response)
                                           :tiled-image img})
                       (logging/log "Finished loading tiled image"))))

;;; Unit Drawing

(let [color-mappings {:yellow :yc
                      :red :os
                      :green :ge
                      :blue :bm
                      :black :bh}]
 (defn draw-unit-at [graphics unit [x y]]
   (let [internal-name (:internal-name unit)
         color (:color unit)
         canvas (:canvas graphics)
         tile (:tiled-image @unit-tiles)
         context (:context graphics)
         kinetic (:kinetic graphics)
         [tx ty] (get (:tile-spec @unit-tiles)
                      [(color-mappings color) internal-name])]
     (. context (drawImage tile
                           tx ty
                           16 16
                           (* x 16)
                           (* y 16)
                           16 16)))))

;;; Path drawing

(defn draw-path [graphics path]
  (let [context (:context graphics)
        newpath (map #(map->canvas % true) (pathfinding/elements path))]
   (. context (beginPath))
   (set! (. context strokeStyle) "rgba(0,0,0,0.5)")
   (set! (. context lineWidth) 4)
   (set! (. context lineCap) "round")
   (set! (. context lineJoin) "round")
   (.moveTo context (ffirst newpath) (second (first newpath)))
   (doseq [[x y] (rest newpath)]
     (.lineTo context x y))
   (. context (stroke))
   (. context (beginPath))
   (let [end (map->canvas (last (pathfinding/elements path)) true)]
    (.arc context
          (first end) (second end)
          2.0
          0
          (* 2 Math/PI)))
   (. context (stroke))))
