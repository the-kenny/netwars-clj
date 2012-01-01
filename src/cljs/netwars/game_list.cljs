(ns netwars.game-list
  (:require [clojure.browser.dom :as dom]
            [netwars.connection :as connection]
            [netwars.game :as game]
            [clojure.browser.event :as event]))

(defn- append-game [id game]
  (let [link (dom/element "a" (str id))]
    (event/listen link :click
                   #(game/join-game id))
   (doto (dom/get-element :gameList)
     (dom/append (dom/element "li" {:class "gameLink"} link)))))

(defmethod connection/handle-response :new-listed-game [response]
  (let [game (:game response)]
    (append-game (:game-id game) (:info game))))

(defmethod connection/handle-response :game-list [response]
  (dom/remove-children :gameList)
  (doseq [[id info] (:games response)]
    (append-game id info)))

(defn request-game-list []
  (connection/send-data {:type :game-list}))
