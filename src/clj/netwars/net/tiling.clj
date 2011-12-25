(ns netwars.net.tiling
  (:use [netwars.aw-map :only [coord]]
        [netwars.net.connection :as connection]
        [netwars.net.otw :as otw])
  (:import java.awt.image.BufferedImage
           java.awt.Graphics2D
           javax.imageio.ImageIO))

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

(defn- make-tile [spec]
  (let [image (BufferedImage. (* 16 (count spec)) 16 BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)
        tile-coords (for [x (range (count spec))
                          :let [[key file] (nth spec x)]]
                      (do (.drawImage graphics (ImageIO/read file)
                                      (* x 16) 0
                                      nil)
                          [key (coord (* x 16) 0)]))]

    (.finalize graphics)
    [(into {} tile-coords) image]))

(defn load-tile [directory]
  (-> directory
      make-tiling-spec
      make-tile))
(alter-var-root #'load-tile memoize)

;;; TODO: Serve via http
(defmethod connection/handle-request :unit-tiles [client request]
  (let [[tilespec tile] (load-tile "resources/pixmaps/units/")]
    (connection/send-data client (assoc request
                                   :tile-spec tilespec
                                   :tiled-image tile))))
