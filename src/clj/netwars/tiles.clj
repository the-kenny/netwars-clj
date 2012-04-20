(ns netwars.tiles
  (:use [netwars.aw-map :only [coord]]))

;; (ns netwars.tiles
;;   (:use [netwars.aw-map :only [coord]]
;;         [netwars.net.connection :as connection]
;;         [netwars.net.otw :as otw])
;;   (:import java.awt.image.BufferedImage
;;            java.awt.Graphics2D
;;            javax.imageio.ImageIO))

;; (defn- make-tiling-spec [directory]
;;   (let [files (filter #(.isFile %) (file-seq (java.io.File. directory)))]
;;     (for [file files]
;;       (let [key (-> file
;;                     (.getPath)
;;                     (.substring (count directory))
;;                     (.split (java.util.regex.Pattern/quote java.io.File/separator))
;;                     vec)]
;;        [(vec (map #(-> % (.replace ".png" "") keyword) key)) file]))))

;; (defn- make-tile [spec]
;;   (let [subs (partition-by ffirst spec)
;;         image (BufferedImage. (* 16 (reduce max (map count subs)))
;;                               (* 16 (count subs))
;;                               BufferedImage/TYPE_INT_ARGB)
;;         graphics (.createGraphics image)
;;         tile-coords (for [y (range (count subs))
;;                           :let [sub (nth subs y)]
;;                           x (range (count sub))
;;                           :let [[key file] (nth sub x)]]
;;                       (do (.drawImage graphics (ImageIO/read file)
;;                                       (* x 16) (* y 32)
;;                                       nil)
;;                           [key (coord (* x 16) (* y 32))]))]

;;     (.finalize graphics)
;;     [(into {} tile-coords) image]))

;; (defn- load-tile [directory]
;;   (-> directory
;;       make-tiling-spec
;;       make-tile))

;; (load-tile "resources/pixmaps/units/")

;; (into {} (for [[k {:keys [x y]}] (first (load-tile "resources/pixmaps/units/"))] [k (list 'coord x y)]))

;; (pprint (into {} (for [[k {:keys [x y]}] (first (load-tile "resources/pixmaps/units/"))] [k (list 'coord x y)])))
;; (javax.imageio.ImageIO/write (second (load-tile "resources/pixmaps/units/")) "png" (java.io.File. "/Users/moritz/units.png"))

(defrecord TileRect [x y width height])

(defn coord->TileRect
  ([c width height]
     (TileRect. (:x c) (:y c) width height))
  ([c] (coord->TileRect c 0 0)))

(defn origin [ta]
  (coord (:x ta) (:y ta)))

(defn tile-filename [tile]
  (:tile-filename tile))

;;; TODO: Find a better way to do this in ClojureScript

;; (require '[clojure.java.io :as io])
;; (import 'java.awt.image.BufferedImage
;;         'java.awt.Graphics2D
;;         'javax.imageio.ImageIO)
;; (defn tile-path [tile]
;;   (-> tile :tile-path io/resource io/file))
;; (defn tile-image [tile]
;;   (ImageIO/read (tile-path tile)))

(defn tile-rect [tile path]
  (when-let [c (get-in tile [:tile-spec (vec path)])]
    (let [[width height] (:tile-size tile)]
      (coord->TileRect c width height))))

(def +unit-meta-tiles+
  {:tile-filename "unit-meta.png"
   :tile-size [16 16]
   :tile-spec {[:ammo]    (coord 0 0)
               [:capture] (coord 16 0)
               [:eight]   (coord 32 0)
               [:five]    (coord 48 0)
               [:four]    (coord 64 0)
               [:fuel]    (coord 80 0)
               [:hidden]  (coord 96 0)
               [:loaded]  (coord 112 0)
               [:nine]    (coord 144 0)
               [:one]     (coord 160 0)
               [:seven]   (coord 176 0)
               [:six]     (coord 192 0)
               [:three]   (coord 208 0)
               [:two]     (coord 224 0)
               [:minus]   (coord 128 0)}})

(def +unit-tiles+
  {:tile-filename "units.png"
   :tile-size [16 16]
   :tile-spec {[:black :anti-air]    (coord 0 16),
               [:black :apc]         (coord 16 16),
               [:black :artillery]   (coord 32 16),
               [:black :b-boat]      (coord 48 16),
               [:black :b-bomb]      (coord 64 16),
               [:black :b-copter]    (coord 80 16),
               [:black :battleship]  (coord 96 16),
               [:black :bomber]      (coord 112 16),
               [:black :carrier]     (coord 128 16),
               [:black :cruiser]     (coord 144 16),
               [:black :fighter]     (coord 160 16),
               [:black :infantry]    (coord 176 16),
               [:black :lander]      (coord 192 16),
               [:black :md-tank]     (coord 208 16),
               [:black :mech]        (coord 224 16)
               [:black :megatank]    (coord 240 16),
               [:black :missiles]    (coord 256 16),
               [:black :neotank]     (coord 272 16),
               [:black :oozium]      (coord 288 16),
               [:black :piperunner]  (coord 304 16),
               [:black :recon]       (coord 320 16),
               [:black :rockets]     (coord 336 16),
               [:black :stealth]     (coord 352 16),
               [:black :submarine]   (coord 368 16),
               [:black :t-copter]    (coord 384 16),
               [:black :tank]        (coord 400 16),
               [:blue :anti-air]     (coord 0 32),
               [:blue :apc]          (coord 16 32),
               [:blue :artillery]    (coord 32 32),
               [:blue :b-boat]       (coord 48 32),
               [:blue :b-bomb]       (coord 64 32),
               [:blue :b-copter]     (coord 80 32),
               [:blue :battleship]   (coord 96 32),
               [:blue :bomber]       (coord 112 32),
               [:blue :carrier]      (coord 128 32),
               [:blue :cruiser]      (coord 144 32),
               [:blue :fighter]      (coord 160 32),
               [:blue :infantry]     (coord 176 32),
               [:blue :lander]       (coord 192 32),
               [:blue :md-tank]      (coord 208 32),
               [:blue :mech]         (coord 224 32),
               [:blue :megatank]     (coord 240 32),
               [:blue :missiles]     (coord 256 32),
               [:blue :neotank]      (coord 272 32),
               [:blue :oozium]       (coord 288 32),
               [:blue :piperunner]   (coord 304 32),
               [:blue :recon]        (coord 320 32),
               [:blue :rockets]      (coord 336 32),
               [:blue :stealth]      (coord 352 32),
               [:blue :submarine]    (coord 368 32),
               [:blue :t-copter]     (coord 384 32),
               [:blue :tank]         (coord 400 32),
               [:green :anti-air]    (coord 0 48),
               [:green :apc]         (coord 16 48),
               [:green :artillery]   (coord 32 48),
               [:green :b-boat]      (coord 48 48),
               [:green :b-bomb]      (coord 64 48),
               [:green :b-copter]    (coord 80 48),
               [:green :battleship]  (coord 96 48),
               [:green :bomber]      (coord 112 48),
               [:green :carrier]     (coord 128 48),
               [:green :cruiser]     (coord 144 48),
               [:green :fighter]     (coord 160 48),
               [:green :infantry]    (coord 176 48),
               [:green :lander]      (coord 192 48),
               [:green :md-tank]     (coord 208 48),
               [:green :mech]        (coord 224 48),
               [:green :megatank]    (coord 240 48),
               [:green :missiles]    (coord 256 48),
               [:green :neotank]     (coord 272 48),
               [:green :oozium]      (coord 288 48),
               [:green :piperunner]  (coord 304 48),
               [:green :recon]       (coord 320 48),
               [:green :rockets]     (coord 336 48),
               [:green :stealth]     (coord 352 48),
               [:green :submarine]   (coord 368 48),
               [:green :t-copter]    (coord 384 48),
               [:red :anti-air]      (coord 0 64),
               [:red :apc]           (coord 16 64),
               [:red :artillery]     (coord 32 64),
               [:red :b-boat]        (coord 48 64),
               [:red :b-bomb]        (coord 64 64),
               [:red :b-copter]      (coord 80 64),
               [:red :battleship]    (coord 96 64),
               [:red :bomber]        (coord 112 64),
               [:red :carrier]       (coord 128 64),
               [:red :cruiser]       (coord 144 64),
               [:red :fighter]       (coord 160 64),
               [:red :infantry]      (coord 176 64),
               [:red :lander]        (coord 192 64),
               [:red :md-tank]       (coord 208 64),
               [:red :mech]          (coord 224 64),
               [:red :megatank]      (coord 240 64),
               [:red :missiles]      (coord 256 64),
               [:red :neotank]       (coord 272 64),
               [:red :oozium]        (coord 288 64),
               [:red :piperunner]    (coord 304 64),
               [:red :recon]         (coord 320 64),
               [:red :rockets]       (coord 336 64),
               [:red :stealth]       (coord 352 64),
               [:red :submarine]     (coord 368 64),
               [:red :t-copter]      (coord 384 64),
               [:red :tank]          (coord 400 64),
               [:yellow :anti-air]   (coord 0 80),
               [:yellow :apc]        (coord 16 80),
               [:yellow :artillery]  (coord 32 80),
               [:yellow :b-boat]     (coord 48 80),
               [:yellow :b-bomb]     (coord 64 80),
               [:yellow :b-copter]   (coord 80 80),
               [:yellow :battleship] (coord 96 80),
               [:yellow :bomber]     (coord 112 80),
               [:yellow :carrier]    (coord 128 80),
               [:yellow :cruiser]    (coord 144 80),
               [:yellow :fighter]    (coord 160 80),
               [:yellow :infantry]   (coord 176 80),
               [:yellow :lander]     (coord 192 80),
               [:yellow :md-tank]    (coord 208 80),
               [:yellow :mech]       (coord 224 80),
               [:yellow :megatank]   (coord 240 80),
               [:yellow :missiles]   (coord 256 80),
               [:yellow :neotank]    (coord 272 80),
               [:yellow :oozium]     (coord 288 80),
               [:yellow :piperunner] (coord 304 80),
               [:yellow :recon]      (coord 320 80),
               [:yellow :rockets]    (coord 336 80),
               [:yellow :stealth]    (coord 352 80),
               [:yellow :submarine]  (coord 368 80),
               [:yellow :t-copter]   (coord 384 80),
               [:yellow :tank]       (coord 400 80),
               [:green :tank]        (coord 400 48)}})


;; (pprint (into {} (for [[k {:keys [x y]}] (first (load-tile "resources/pixmaps/units/"))] [k (list 'coord x y)])))
;; (javax.imageio.ImageIO/write (second (load-tile "resources/pixmaps/units/")) "png" (java.io.File. "/Users/moritz/units.png"))

(def +terrain-tiles+
  {:tile-filename "terrain.png"
   :tile-size [16 32]
   :tile-spec {[:beach :d]                       (coord 0 0)
               [:beach :dr]                      (coord 16 0)
               [:beach :l]                       (coord 32 0)
               [:beach :ld]                      (coord 48 0)
               [:beach :ldr]                     (coord 64 0)
               [:beach :r]                       (coord 80 0)
               [:beach :u]                       (coord 96 0)
               [:beach :udr]                     (coord 112 0)
               [:beach :ul]                      (coord 128 0)
               [:beach :uld]                     (coord 144 0)
               [:beach :ulr]                     (coord 160 0)
               [:beach :ur]                      (coord 176 0)
               [:bridge :lr]                     (coord 192 0)
               [:bridge :ud]                     (coord 208 0)
               [:buildings :airport :black]      (coord 240 0)
               [:buildings :airport :blue]       (coord 256 0)
               [:buildings :airport :green]      (coord 272 0)
               [:buildings :airport :red]        (coord 288 0)
               [:buildings :airport :white]      (coord 304 0)
               [:buildings :airport :yellow]     (coord 320 0)
               [:buildings :base :black]         (coord 336 0)
               [:buildings :base :blue]          (coord 352 0)
               [:buildings :base :green]         (coord 368 0)
               [:buildings :base :red]           (coord 384 0)
               [:buildings :base :white]         (coord 400 0)
               [:buildings :base :yellow]        (coord 416 0)
               [:buildings :city :black]         (coord 432 0)
               [:buildings :city :blue]          (coord 448 0)
               [:buildings :city :green]         (coord 464 0)
               [:buildings :city :red]           (coord 480 0)
               [:buildings :city :white]         (coord 496 0)
               [:buildings :city :yellow]        (coord 512 0)
               [:buildings :headquarter :black]  (coord 544 0)
               [:buildings :headquarter :blue]   (coord 560 0)
               [:buildings :headquarter :green]  (coord 576 0)
               [:buildings :headquarter :red]    (coord 592 0)
               [:buildings :headquarter :yellow] (coord 608 0)
               [:buildings :lab :black]          (coord 624 0)
               [:buildings :lab :blue]           (coord 640 0)
               [:buildings :lab :green]          (coord 656 0)
               [:buildings :lab :red]            (coord 672 0)
               [:buildings :lab :white]          (coord 688 0)
               [:buildings :lab :yellow]         (coord 704 0)
               [:buildings :port :black]         (coord 720 0)
               [:buildings :port :blue]          (coord 736 0)
               [:buildings :port :green]         (coord 752 0)
               [:buildings :port :red]           (coord 768 0)
               [:buildings :port :white]         (coord 784 0)
               [:buildings :port :yellow]        (coord 800 0)
               [:buildings :silo :white]         (coord 816 0)
               [:buildings :silo :white_shot]    (coord 832 0)
               [:buildings :tower :black]        (coord 848 0)
               [:buildings :tower :blue]         (coord 864 0)
               [:buildings :tower :green]        (coord 880 0)
               [:buildings :tower :red]          (coord 896 0)
               [:buildings :tower :white]        (coord 912 0)
               [:buildings :tower :yellow]       (coord 928 0)
               [:forest]                         (coord 944 0)
               [:mountain :big]                  (coord 960 0)
               [:mountain :small]                (coord 976 0)
               [:pipe :d]                        (coord 992 0)
               [:pipe :dr]                       (coord 1008 0)
               [:pipe :l]                        (coord 1024 0)
               [:pipe :ld]                       (coord 1040 0)
               [:pipe :lr]                       (coord 1056 0)
               [:pipe :r]                        (coord 1072 0)
               [:pipe :u]                        (coord 1088 0)
               [:pipe :ud]                       (coord 1104 0)
               [:pipe :ul]                       (coord 1120 0)
               [:pipe :ur]                       (coord 1136 0)
               [:plain]                          (coord 1152 0)
               [:reef]                           (coord 1168 0)
               [:river :dr]                      (coord 1184 0)
               [:river :ld]                      (coord 1200 0)
               [:river :ldr]                     (coord 1216 0)
               [:river :lr]                      (coord 1232 0)
               [:river :mouth :d]                (coord 1248 0)
               [:river :mouth :l]                (coord 1264 0)
               [:river :mouth :r]                (coord 1280 0)
               [:river :mouth :u]                (coord 1296 0)
               [:river :ud]                      (coord 1312 0)
               [:river :udr]                     (coord 1328 0)
               [:river :ul]                      (coord 1344 0)
               [:river :uld]                     (coord 1360 0)
               [:river :uldr]                    (coord 1376 0)
               [:river :ulr]                     (coord 1392 0)
               [:river :ur]                      (coord 1408 0)
               [:seaside :corner :dr]            (coord 1424 0)
               [:seaside :corner :ld]            (coord 1440 0)
               [:seaside :corner :ul]            (coord 1456 0)
               [:seaside :corner :ur]            (coord 1472 0)
               [:seaside :d]                     (coord 1488 0)
               [:seaside :dr]                    (coord 1504 0)
               [:seaside :l]                     (coord 1520 0)
               [:seaside :ld]                    (coord 1536 0)
               [:seaside :ldr]                   (coord 1552 0)
               [:seaside :lr]                    (coord 1568 0)
               [:seaside :r]                     (coord 1584 0)
               [:seaside :u]                     (coord 1600 0)
               [:seaside :ud]                    (coord 1616 0)
               [:seaside :udr]                   (coord 1632 0)
               [:seaside :ul]                    (coord 1648 0)
               [:seaside :uld]                   (coord 1664 0)
               [:seaside :uldr]                  (coord 1680 0)
               [:seaside :ulr]                   (coord 1696 0)
               [:seaside :ur]                    (coord 1712 0)
               [:segment-pipe :lr]               (coord 1728 0)
               [:segment-pipe :ud]               (coord 1744 0)
               [:street :dr]                     (coord 1760 0)
               [:street :ld]                     (coord 1776 0)
               [:street :ldr]                    (coord 1792 0)
               [:street :lr]                     (coord 1808 0)
               [:street :ud]                     (coord 1824 0)
               [:street :udr]                    (coord 1840 0)
               [:street :ul]                     (coord 1856 0)
               [:street :uld]                    (coord 1872 0)
               [:street :uldr]                   (coord 1888 0)
               [:street :ulr]                    (coord 1904 0)
               [:street :ur]                     (coord 1920 0)
               [:water]                          (coord 1936 0)
               [:wreckage :lr]                   (coord 1952 0)
               [:wreckage :ud]                   (coord 1968 0)
               [:wreckage]                       (coord 1984 0)}})
