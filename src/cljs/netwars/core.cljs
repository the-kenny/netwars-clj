(ns netwars.core
  (:require [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [goog.dom.classes :as classes]
            [goog.Timer :as Timer]
            [netwars.drawing :as drawing]
            [netwars.connection :as connection]
            [netwars.game-list :as game-list]
            [netwars.game :as game]
            [netwars.logging :as logging]

            ;; Load all crossovers prevent strippint
            [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.damagecalculator :as damagecalculator]
            [netwars.game-board :as game-board]
            [netwars.map-utils :as map-utils]))

;;; Logging Stuff

(defn set-connection-status [status]
  (dom/set-text (dom/get-element :connectionIndicator) status))

;;; Implement drawing the requested map

(defn on-load-map-submit []
  (logging/log "Requesting new map from server")
  (game/start-new-game (.-value (dom/get-element :mapName))))

(connection/on-open
 #(let [elem (dom/get-element :connectionIndicator)]
    (classes/set elem "connected")
    (dom/set-text elem "connected")))
(connection/on-close
 #(let [elem (dom/get-element :connectionIndicator)]
    (classes/set elem "disconnected")
    (dom/set-text elem "closed...")))

(def board-context (drawing/make-graphics (dom/get-element :gameBoard)))

(event/listen (dom/get-element :mapForm)
               :submit
               #(do (on-load-map-submit)
                    (.preventDefault %)))

;;; Request game list on open
(connection/on-open #(connection/send-data {:type :helo}))

(game/setup-event-listeners board-context)
(drawing/set-drawing-function! board-context game/draw-game)
;;(drawing/start-animation board-context)

 ;;; Network stuff

(let [host (.. js/window -location -host)]
  (connection/open-socket (str "ws://" host "/socket")))


;;; TODO: Use animation when enormous cpu usage is fixed
(let [t (goog.Timer. 100)]
  (event/listen t "tick" #(drawing/redraw board-context))
  (.start t))
