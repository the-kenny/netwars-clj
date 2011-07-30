(ns netwars.net.tiling
  (:use [netwars.aw-map :only [coord]]
        [clojure.contrib.def :only [defn-memo]])
  (:import java.awt.image.BufferedImage
           java.awt.Graphics2D
           javax.imageio.ImageIO))

(defn- make-tiling-spec [directory]
  (let [files (filter #(.isFile %) (file-seq (java.io.File. directory)))]
    (for [file files]
      [(-> file (.getPath) (.substring (count directory))) file])))

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
