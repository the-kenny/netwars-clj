(ns netwars.net.rest
  (:use [netwars.net.game-server :as server]
        compojure.core
        [clojure.data.json :as json]))

;;; Serialization

(extend-type java.util.UUID
  json/Write-JSON
  (write-json [object out escape-unicode?]
    (json/write-json (str object) out escape-unicode?)))

(extend-type netwars.aw_game.AwGame
  json/Write-JSON
  (write-json [game out escape-unicode?]
    (let [object (select-keys game [:info])]
     (json/write-json (merge object
                             {:players @(:players game)
                              :current-player-index @(:current-player-index game)
                              :moves (rest @(:moves game))})
                      out escape-unicode?))))

;;; Methods

(defn main []
  (json-str {:api-version 1.0}))

(defn games []
  (let [ids (keys @server/running-games)]
    (json-str {:count (count ids)
               :ids (vec ids)})))

(defn game [id]
  (json-str
   (if-let [uuid (try (java.util.UUID/fromString id) (catch Exception _ nil))]
     (if-let [game (server/get-game uuid)]
       game
       {:error :not-found})
     {:error :invalid-uuid})))

;;; Compojure routes

(defroutes api-routes
  (GET "/" [] (main))
  (GET "/games" [] (games))
  (GET "/game/:id" [id] (game id)))
