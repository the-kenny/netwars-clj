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
            [cljs.reader :as reader]

            [netwars.game-drawer :as game-drawer]
            [netwars.test-games :as test-games]

            [netwars.menus.generic :as menu]
            [netwars.menus.unit-menu :as unit-menu]
            [netwars.menus.attack-menu :as attack-menu]

            ;; Load all crossovers prevent stripping
            [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.damagecalculator :as damagecalculator]
            [netwars.game-board :as game-board]
            [netwars.map-utils :as map-utils]
            [netwars.tile-drawer :as tile-drawer]))

(def current-game (atom nil))
(def current-action-menu (atom nil))

(defn unit-action-wait [game c]
  ;; TODO: Really make the unit wait
  (logging/log "Waiting...")
  (swap! current-game aw-game/deselect-unit)
  ;; TODO: Dismissal shouldn't be done in every action-fn
  (swap! current-action-menu menu/hide-menu))


(defn show-unit-action-menu [game c unit]
  (let [menu (unit-menu/unit-action-menu game c {:wait #(unit-action-wait game c)})]
    (menu/display-menu menu (dom/get-element :mapBox) (game-drawer/coord->canvas c))
    (reset! current-action-menu menu))
  ;; Return nil to indicate no re-draw is needed
  nil)

(defn show-attack-menu [game c]
  (let [menu (attack-menu/attack-menu game c {:attack #(unit-action-wait game c)
                                              :cancel #(swap! current-action-menu menu/hide-menu)})]
    (menu/display-menu menu (dom/get-element :mapBox) (game-drawer/coord->canvas c))
    (reset! current-action-menu menu))
  ;; Return nil to indicate no re-draw is needed
  nil)


(defn own-unit-clicked [game c unit]
  (cond
   ;; Bug: (= c null) => crash; (= null c) => false
   (= (aw-game/selected-coordinate game) c) (show-unit-action-menu game c unit)
   true (aw-game/select-unit game c)))

(defn enemy-unit-clicked [game c unit]
  (when-let [att-coord (aw-game/selected-coordinate game)]
    (when (aw-game/attack-possible? game att-coord c)
      (logging/log "Attack!")
      ;; TODO: Show attack menu
      (show-attack-menu game c))))

(defn unit-clicked [game c]
  (let [unit (-> game :board (game-board/get-unit c))]
    (if (= (:color unit) (:color (aw-game/current-player game)))
      (own-unit-clicked game c unit)
      (enemy-unit-clicked game c unit))))

(defn terrain-clicked [game c]
  (let [terrain (-> game :board (game-board/get-terrain c))]
    (logging/log (apply str (map name (seq terrain))))
    ;; Return nil to indicate no re-draw is needed
    nil))

(defn clicked-on [c]
  (when (and @current-game (not @current-action-menu))
    (let [game @current-game
          newgame (let [board (:board game)
                        terrain (game-board/get-terrain board c)
                        unit (game-board/get-unit board c)]
                    (cond
                     unit    (unit-clicked    game c)
                     terrain (terrain-clicked game c)
                     true game))]
      ;; Don't redraw the game when the state hasn't changed
      ;; TODO: This shouldn't be necessary when everything is opimized
      (when newgame
        (reset! current-game newgame)))))

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
