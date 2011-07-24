(ns netwars.core
  (:require
   [goog.events :as events]
   [goog.dom :as dom]
   [goog.json :as json]))

(defn set-connection-status [status]
  (dom/setTextContent (dom/getElement "connectionIndicator") status))

(defn log-message [message]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil message)))

(let [i (atom 0)]
 (defn on-message [event]
   (let [obj (json/parse (.data event))
         encoded-map (.map-image obj)]
     (log-message (str "got message... " @i))
     (when encoded-map
      (set! (. (dom/getElement "image") src) encoded-map))
     (swap! i inc))))

(defn start-socket [uri]
  (let [ws (js/WebSocket. uri)]
    ;; (set! (. ws onopen) #(set-connection-status "opened!"))
    (events/listen ws "open" #(set-connection-status "opened!"))
    (events/listen ws "close" #(set-connection-status "closed!"))
    ;; (events/listen ws "message" on-message)
    (set! (. ws onmessage) on-message)
    ))

(defn on-load []
  (start-socket "ws://localhost:8080"))
