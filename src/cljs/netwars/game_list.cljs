(ns netwars.game-list
  (:require [goog.dom :as dom]
            [netwars.connection :as connection]
            [netwars.game :as game]
            [goog.events :as events]))

(defn- append-game [server id game]
  (let [link (dom/createDom "a" nil (str id))]
    (events/listen link goog.events.EventType/CLICK
                   #(game/join-game server id))
   (doto (dom/getElement "gameList")
     (.appendChild (dom/createDom "li" "gameLink" link)))))

(defmethod connection/handle-response :game-list [server response]
  (doseq [[id info] (:games response)]
    (append-game server id info)))

(defn request-game-list [server]
  (connection/send-data server {:type :game-list}))
