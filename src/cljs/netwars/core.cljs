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

(events/listen (dom/getElement "request-button") events/EventType.CLICK
               #(do (connection/log "Clicked request-button")
                    (request-map-data "7330.aws")))

;(request-map-data "7330.aws")
