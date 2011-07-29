(ns netwars.core
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [netwars.drawing :as drawing]
            [netwars.connection :as connection]))

;;; Logging Stuff

(defn log-message [message]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil message)))

(connection/on-open
  #(set! (. (dom/getElement "connectionIndicator") textContent) "connected"))
(connection/on-close
 #(set! (. (dom/getElement "connectionIndicator") textContent) "closed..."))

(def board-context (drawing/make-graphics (dom/getElement "gameBoard")))

;;; Network stuff

(defn set-connection-status [status]
  (dom/setTextContent (dom/getElement "connectionIndicator") status))

(def socket (connection/open-socket "ws://localhost:8080"))

;;; Implement drawing the requested map
(let [request-name "request-map"]
 (defmethod connection/handle-response request-name [message]
   (drawing/draw-terrain board-context
                         (get message "map-data")))

 (defn request-map-data [m]
   (connection/send-data socket {"type" request-name, "map" m})))

(defn on-load-map-submit []
  (connection/log "Requesting new map from server")
  (request-map-data (.value (dom/getElement "mapName"))))

(events/listen (dom/getElement "mapForm")
               events/EventType.SUBMIT
               #(do (on-load-map-submit)
                    (. % (preventDefault))))
