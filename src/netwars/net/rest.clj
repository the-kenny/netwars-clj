(ns netwars.net.rest
  (:use compojure.core
        ;; [netwars.net.game-server :as server]
        [clojure.java.io :only [input-stream output-stream]])
  (:require [netwars.aw-game :as aw-gameq]
            [netwars.damagetable :as damagetable]
            [netwars.unit-loader :as unit-loader]
            [netwars.game-creator :as game-creator]
            [netwars.net.otw :as otw]
            [netwars.map-drawer :as map-drawer]

            [ring.middleware.edn :as edn])
  (:import [netwars.aw_map Coordinate]))

;;; Methods

(defn main []
  (otw/encode-data {:api-version 0.1}))

(defn damagetable []
  (otw/encode-data (damagetable/load-damagetable "resources/damagetable.xml")))
;; (alter-var-root #'damagetable memoize)

(defn unit-spec []
  (otw/encode-data (unit-loader/load-units "resources/units.xml")))
;; (alter-var-root #'unit-spec memoize)

;;; Map stuff

(defn- map-url [map-name]
  (str "/api/render-map/" map-name))

(defn render-map [map-name]
  ;; TODO: This performs badly
  (let [game (game-creator/make-game {} map-name)
        image (map-drawer/render-terrain-board (-> game :board :terrain))
        in (java.io.PipedInputStream.)]
    ;; Now this is fun stuff:
    ;; We throw off a future which writes data to the output stream
    ;; which then feeds this data to in which is read by Ring
    (future (with-open [out (java.io.PipedOutputStream. in)]
              (javax.imageio.ImageIO/write image "png" out)))
    {:status 200
     :headers {"Content-Type" "image/png"}
     :body in}))

(defn make-game [map-name]
  (->  (game-creator/make-game {} map-name)
       (assoc  :map-url (map-url map-name))
       (otw/encode-data)))

;;; Compojure routes

(defroutes api-handler
  (GET "/"                     [] (main))
  (GET "/damagetable"          [] (damagetable))
  (GET "/unit-spec"            [] (unit-spec))
  (GET "/new-game/:map-name"   [map-name] (make-game map-name))
  (GET "/render-map/:map-name" [map-name] (render-map map-name)))

(def api
  (-> api-handler
      edn/wrap-edn-params))
