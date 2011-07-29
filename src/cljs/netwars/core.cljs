(ns netwars.core
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [netwars.drawing :as drawing]
            [netwars.connection :as connection]))

;;; Logging Stuff

(defn log-message [message]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil message)))

(def board-context (drawing/make-graphics (dom/getElement "gameBoard")))

;;; Network stuff

(defn set-connection-status [status]
  (dom/setTextContent (dom/getElement "connectionIndicator") status))

(def socket (connection/open-socket "ws://localhost:8080"))

(defn request-map-data [m]
  (connection/send-data socket {"type" "request-map", "map" m}
                        (fn [obj]
                          (drawing/draw-terrain board-context
                                                (get obj "map-data")))))

(defn on-load-map-submit []
  (connection/log "Requesting new map from server")
  (request-map-data (.value (dom/getElement "mapName"))))

(events/listen (dom/getElement "mapForm")
               events/EventType.SUBMIT
               #(do (on-load-map-submit)
                    (. % (preventDefault))))
;(request-map-data "7330.aws")
