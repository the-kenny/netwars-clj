(ns netwars.net.rest
  (:use compojure.core
        clojure.data.json
        ;; [netwars.net.game-server :as server]
        [clojure.java.io :only [input-stream output-stream]])
  (:require [netwars.aw-game :as aw-gameq]
            [netwars.damagetable :as damagetable]
            [netwars.unit-loader :as unit-loader]
            [netwars.game-creator :as game-creator]
            [netwars.net.otw :as otw]
            [netwars.map-drawer :as map-drawer])
  (:import [netwars.aw_map Coordinate]))

;;; Serialization

(extend-type java.util.UUID
  Write-JSON
  (write-json [object out escape-unicode?]
    (write-json (str object) out escape-unicode?)))

;; (extend-type netwars.aw_game.AwGame
;;   Write-JSON
;;   (write-json [object out escape-unicode?]
;;     (write-json object out escape-unicode?)))

(extend-type Coordinate
  Write-JSON
  (write-json [c out escape-unicode?]
    (write-json (list 'coord (:x c) (:y c))
                out
                escape-unicode?)))

;;; Methods

(defn main []
  (json-str {:api-version 0.1}))

(defn damagetable []
  (json-str (damagetable/load-damagetable "resources/damagetable.xml")))
;; (alter-var-root #'damagetable memoize)

(defn unit-spec []
  (json-str (unit-loader/load-units "resources/units.xml")))
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
       (dissoc :moves)
       (assoc  :map-url (map-url map-name))
       (otw/encode-data)))

;; (defn games []
;;   (let [ids (keys @server/running-games)]
;;     (json-str {:count (count ids)
;;                :ids (vec ids)})))

;; (defn game [id]
;;   (json-str
;;    (if-let [uuid (try (java.util.UUID/fromString id) (catch Exception _ nil))]
;;      (if-let [game (server/get-game uuid)]
;;        game
;;        {:error :not-found})
;;      {:error :invalid-uuid})))

;;; Compojure routes

(defroutes api-routes
  (GET "/" [] (main))
  (GET "/damagetable.json" [] (damagetable))
  (GET "/unit-spec.json" [] (unit-spec))
  (GET "/new-game/:map-name" [map-name] (make-game map-name))
  (GET "/render-map/:map-name" [map-name] (render-map map-name))
  ;; (GET "/games" [] (games))
  ;; (GET "/game/:id" [id] (game id))
  )
