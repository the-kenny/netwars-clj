(ns netwars.core
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [goog.dom.classes :as classes]
            [goog.Timer :as Timer]
            [netwars.drawing :as drawing]
            [netwars.connection :as connection]
            [netwars.game-list :as game-list]
            [netwars.game :as game]
            [netwars.logging :as logging]))

;;; Logging Stuff

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

(let [host (.. js/window location host)]
 (connection/open-socket (str "ws://" host "/socket")))

;;; Implement drawing the requested map

(defn on-load-map-submit []
  (logging/log "Requesting new map from server")
  (game/start-new-game (.value (dom/getElement "mapName"))))

(events/listen (dom/getElement "mapForm")
               events/EventType.SUBMIT
               #(do (on-load-map-submit)
                    (. % (preventDefault))))

;;; Request game list on open
(connection/on-open #(connection/send-data {:type :helo}))

(game/setup-event-listeners board-context)
(drawing/set-drawing-function! board-context game/draw-game)
;;(drawing/start-animation board-context)

;;; TODO: Use animation when enormous cpu usage is fixed
(let [t (goog.Timer. 100)]
  (events/listen t goog.Timer/TICK #(drawing/redraw board-context))
  (. t (start)))
