(ns netwars.game-list
  (:require [goog.dom :as dom]
            [netwars.connection :as connection]
            [netwars.game :as game]
            [goog.events :as events]))

(defn- append-game [id game]
  (let [link (dom/createDom "a" nil (str id))]
    (events/listen link goog.events.EventType/CLICK
                   #(game/join-game id))
   (doto (dom/getElement "gameList")
     (.appendChild (dom/createDom "li" "gameLink" link)))))

(defmethod connection/handle-response :new-listed-game [response]
  (let [game (:game response)]
    (append-game (:game-id game) (:info game))))

(defmethod connection/handle-response :game-list [response]
  (dom/removeChildren (dom/getElement "gameList"))
  (doseq [[id info] (:games response)]
    (append-game id info)))

(defn request-game-list []
  (connection/send-data {:type :game-list}))
