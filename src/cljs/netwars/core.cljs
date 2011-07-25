(ns netwars.core
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [goog.json :as json]
            [netwars.drawing :as drawing]))

;;; Logging Stuff

(defn log-message [message]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil message)))

(def board-context (drawing/make-graphics (dom/getElement "gameBoard")))

;;; Network stuff

(defn set-connection-status [status]
  (dom/setTextContent (dom/getElement "connectionIndicator") status))

(defn on-message [event]
  (let [obj (json/parse (.data event))
        encoded-map (.map-image obj)
        units (.units obj)]
    (when-let [message (.message obj)]
     (log-message (str "got message: " message)))
    (log-message (str (.data event)))
    (when units
      (log-message (str "got " units " units")))
    (when encoded-map
      (drawing/draw-terrain board-context encoded-map))))

(defn start-socket [uri]
  (let [ws (js/WebSocket. uri)]
    ;; (set! (. ws onopen) #(set-connection-status "opened!"))
    (events/listen ws "open" #(set-connection-status "connected"))
    (events/listen ws "close" #(set-connection-status "closed"))
    ;; (events/listen ws "message" on-message)
    (set! (. ws onmessage) on-message)))

(start-socket "ws://localhost:8080")
