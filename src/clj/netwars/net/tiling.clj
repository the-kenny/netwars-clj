(ns netwars.net.tiling
  (:use [netwars.aw-map :only [coord]]
        [netwars.net.connection :as connection]
        [clojure.contrib.def :only [defn-memo]]
        [clojure.string :as string])
  (:import java.awt.image.BufferedImage
           java.awt.Graphics2D
           javax.imageio.ImageIO
           ;; java.io.File
           ))

(defn- make-tiling-spec [directory]
  (let [files (filter #(.isFile %) (file-seq (java.io.File. directory)))]
    (for [file files]
      (let [key (-> file
                    (.getPath)
                    (.substring (count directory))
                    (.split (java.util.regex.Pattern/quote java.io.File/separator))
                    vec
                    (update-in [1] #(.substring % 0 (- (count %) 4))))]
       [(vec (map keyword key)) file]))))

;;; Returns [tile-spec tiled-image]
;;; tile-spec maps keys (path-strings) to coordinates on the tiles-image
;;; Every subtile is 16x16px and its upper-left is at the coordinate saved in tile-spec
(defn- make-tile [spec]
  (let [grouped-spec (partition-by (fn [[k v]] (take 3 k)) spec)
        image (BufferedImage. (* 16 (count grouped-spec))
                              (* 16 (apply max (map count grouped-spec)))
                              BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)
        tile-coords (for [x (range (count grouped-spec))
                          :let [spec (nth grouped-spec x)]
                          y (range (count spec))
                          :let [[key file] (nth spec y)]]
                      (do (.drawImage graphics (ImageIO/read file)
                                      (* x 16) (* y 16)
                                      nil)
                          [key (coord (* x 16) (* y 16))]))]

    (.finalize graphics)
    [(into {} tile-coords) image]))

(defn-memo load-tile [directory]
  (-> directory
      make-tiling-spec
      make-tile))

(defmethod connection/handle-request :unit-tiles [client request]
  (let [[tilespec tile] (load-tile "resources/pixmaps/units/")]
    (connection/send-data
     client
     (assoc request
       :tile-spec (into {}
                        (for [[k c] tilespec]
                          [k (connection/encode-coordinate c)]))
       :tiled-image (connection/image-to-base64 tile)))))
