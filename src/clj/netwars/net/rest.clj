(ns netwars.net.rest
  (:use compojure.core
        clojure.data.json
        ;; [netwars.net.game-server :as server]
        )
  (:require [netwars.aw-game :as aw-gameq]
            [netwars.damagetable :as damagetable]
            [netwars.unit-loader :as unit-loader]
            [netwars.game-creator :as game-creator]
            [netwars.net.otw :as otw])
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

(defn make-game [map-name]
  (otw/encode-data (dissoc (game-creator/make-game {} (str "maps/" map-name))
                           :moves))) ;; (defn games []
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
  ;; (GET "/games" [] (games))
  ;; (GET "/game/:id" [id] (game id))
  )
