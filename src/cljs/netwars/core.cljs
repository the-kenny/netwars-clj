(ns netwars.core
  (:require [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [clojure.browser.repl :as repl]
            [goog.dom.classes :as classes]
            [goog.Timer :as Timer]

            [netwars.logging :as logging]
            [netwars.tiles :as tiles]
            [netwars.net.otw :as otw]

            [clojure.browser.net :as net]
            [goog.net :as gnet]
            [cljs.reader :as reader]

            [netwars.game-drawer :as game-drawer]
            [netwars.test-games :as test-games]

            ;; Load all crossovers prevent stripping
            [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.damagecalculator :as damagecalculator]
            [netwars.game-board :as game-board]
            [netwars.map-utils :as map-utils]
            [netwars.tile-drawer :as tile-drawer]))

(def current-game (atom nil))

(defn own-unit-clicked [game c unit]
  (cond
   ;; Bug: (= c null) => crash; (= null c) => false
   (= (aw-game/selected-coordinate game) c) (aw-game/deselect-unit game)
   true (aw-game/select-unit game c)))

(defn enemy-unit-clicked [game c unit]
  game)

(defn unit-clicked [game c]
  (let [unit (-> game :board (game-board/get-unit c))]
    (if (= (:color unit) (:color (aw-game/current-player game)))
     (own-unit-clicked game c unit)
     (enemy-unit-clicked game c unit))))

(defn terrain-clicked [game c]
  (let [terrain (-> game :board (game-board/get-terrain c))]
    (logging/log (apply str (map name (seq terrain))))
    game))

(defn clicked-on [c]
  (when @current-game
    (swap! current-game
           (fn [game]
             (let [board (:board game)
                   terrain (game-board/get-terrain board c)
                   unit (game-board/get-unit board c)]
               (cond
                unit    (unit-clicked    game c)
                terrain (terrain-clicked game c)
                true game))))))

(defn register-handlers [canvas]
  (event/listen canvas :click
                (fn [event]
                  (clicked-on (game-drawer/canvas->coord
                               (aw-map/coord (.-offsetX event) (.-offsetY event))))))
  ;; We use add-watch to redraw the canvas every time the state changes
  (add-watch current-game :redrawer
           (fn [key ref old new]
             (game-drawer/draw-game canvas new))))

(register-handlers (dom/get-element :gameBoard))

(reset! current-game netwars.test-games/basic-game)

;;; Testing functions

(defn load-test-game []
  (logging/log "Loading game...")
  (goog.net.XhrIo/send "http://localhost:8080/api/new-game/7330.aws"
                       (fn [e]
                         (logging/log "Got data, started reading in")
                         (let [game (-> e
                                        .-target
                                        .getResponseText
                                        otw/decode-data
                                        aw-game/map->AwGame)]
                           (reset! current-game game))
                         (logging/log "Loaded game."))))

(defn repl-connect []
 (repl/connect "http://localhost:9000/repl"))
