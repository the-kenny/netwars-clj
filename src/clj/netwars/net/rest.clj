(ns netwars.net.rest
  (:use compojure.core
        clojure.data.json
        ;; [netwars.net.game-server :as server]
        )
  (:require [netwars.aw-game :as aw-gameq]
            [netwars.damagetable :as damagetable]
            [netwars.unit-loader :as unit-loader]))

;;; Serialization

(extend-type java.util.UUID
  Write-JSON
  (write-json [object out escape-unicode?]
    (write-json (str object) out escape-unicode?)))

(extend-type netwars.aw_game.AwGame
  Write-JSON
  (write-json [game out escape-unicode?]
    (let [object (select-keys game [:info])]
      (write-json (merge object
                         {:players @(:players game)
                          :current-player-index @(:current-player-index game)
                          :moves (rest @(:moves game))})
                  out escape-unicode?))))

;;; Methods

(defn main []
  (json-str {:api-version 0.1}))

(defn damagetable [name]
  (json-str (damagetable/load-damagetable "resources/damagetable.xml")))
(alter-var-root #'damagetable memoize)

(defn unit-spec [name]
  (json-str (unit-loader/load-units "resources/units.xml")))
(alter-var-root #'unit-spec memoize)

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
  (GET "/damagetable/" [] (damagetable name))
  (GET "/unit-spec/" [] (unit-spec name))
  ;; (GET "/games" [] (games))
  ;; (GET "/game/:id" [id] (game id))
  )
