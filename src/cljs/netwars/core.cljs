(ns netwars.core
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [goog.dom.classes :as classes]
            [netwars.drawing :as drawing]
            [netwars.connection :as connection]
            [netwars.game-list :as game-list]
            [netwars.game :as game]))

;;; Logging Stuff

(defn log-message [message]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil message)))

(defn set-connection-status [status]
  (dom/setTextContent (dom/getElement "connectionIndicator") status))

(connection/on-open
  #(let [elem (dom/getElement "connectionIndicator")]
     (classes/set elem "connected")
     (dom/setTextContent elem "connected")))
(connection/on-close
  #(let [elem (dom/getElement "connectionIndicator")]
     (classes/set elem "disconnected")
     (dom/setTextContent elem "closed...")))

(def board-context (drawing/make-graphics (dom/getElement "gameBoard")))

;;; Network stuff

(def socket (connection/open-socket "ws://moritz-macbook.local:8080"))

;;; Implement drawing the requested map
(let [request-name :request-map]
 (defmethod connection/handle-response request-name [_ message]
   (drawing/draw-terrain board-context
                         (get message :map-data)))

 (defn request-map-data [m]
   (connection/send-data socket {:type request-name,
                                 :map m})))

(defn on-load-map-submit []
  (connection/log "Requesting new map from server")
  (game/start-new-game socket (.value (dom/getElement "mapName"))))

(events/listen (dom/getElement "mapForm")
               events/EventType.SUBMIT
               #(do (on-load-map-submit)
                    (. % (preventDefault))))

(defmethod connection/handle-response :unit-data [_ data]
  (connection/log "got " (count (:units data)) " units")
  (doseq [[c u] (:units data)]
    (connection/log "drawing unit " (name (:internal-name u)) " " (name (:color u)))
    (drawing/draw-unit-at board-context
                          (first c) (second c)
                          u)))

;;; Request game list on open
(connection/on-open game-list/request-game-list)
(connection/on-open drawing/request-unit-tiles)
