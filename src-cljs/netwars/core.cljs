(ns netwars.core
  (:require [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [clojure.browser.repl :as repl]
            [goog.dom.classes :as classes]
            [goog.events :as gevents]

            [netwars.logging :as logging]
            [netwars.tiles :as tiles]
            [netwars.net.otw :as otw]
            [netwars.pathfinding :as pathfinding]

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

;;; TODO: Function for changing game-state instead of
;;; (reset current-game ...)
(def current-game (atom nil))
(def current-action-menu (atom nil))

;;; Unit Actions (Attack, Wait, Capture, ...)

(defn action-cancel
  "Generic action for canceling actions.
   Deselects current unit and hides the action menu."
  []
  (swap! current-game aw-game/deselect-unit)
  (swap! current-action-menu menu/hide-menu))

(defn unit-action-wait
  "Deselects and 'Waits' the current unit."
  [game c]
  (reset! current-game (aw-game/wait-unit game))
  ;; TODO: Dismissal shouldn't be done in every action-fn
  (swap! current-action-menu menu/hide-menu))

(defn unit-action-attack
  "Functionally just hides the action menu. In the background this
  function applies the game-state given as argument because it after
  moving the current state is just drawn, not applied."
  [game c]
  (reset! current-game (dissoc game :current-path))
  (swap! current-action-menu menu/hide-menu))

(defn unit-action-capture
  "Action to capture buildings.
   Deselects current unit and hides action menu."
  [game c]
  {:pre (game-board/capture-possible? (:board game) c)}
  (swap! current-game #(-> game
                           (aw-game/capture-building c)
                           aw-game/deselect-unit))
  ;; TODO: Dismissal shouldn't be done in every action-fn
  (swap! current-action-menu menu/hide-menu))

(defn show-unit-action-menu [game c unit]
  (let [menu (unit-menu/unit-action-menu game c {:wait    #(unit-action-wait game c)
                                                 :attack  #(unit-action-attack game c)
                                                 :capture #(unit-action-capture game c)
                                                 :cancel  #(action-cancel)}
                                         unit)]
    (menu/display-menu menu (dom/get-element :mapBox) (game-drawer/coord->canvas c))
    (reset! current-action-menu menu))
  ;; Return nil to indicate no re-draw is needed
  nil)

(defn attack-action-attack [game c]
  (swap! current-game #(let [att (aw-game/selected-coordinate %)
                             def c]
                         (-> game
                             (dissoc :moving-disabled)
                             (aw-game/perform-attack att def)
                             (aw-game/deselect-unit))))
  ;; TODO: Dismissal shouldn't be done in every action-fn
  (swap! current-action-menu menu/hide-menu))

(defn show-attack-menu [game c]
  (let [menu (attack-menu/attack-menu game c {:attack #(attack-action-attack game c)
                                              :cancel #(action-cancel)})]
    (menu/display-menu menu (dom/get-element :mapBox) (game-drawer/coord->canvas c))
    (reset! current-action-menu menu))
  ;; Return nil to indicate no re-draw is needed
  nil)


;;; Unit info functions

(defn show-unit-info [unit]
  (dom/set-text (dom/get-element :unit-hp)
                (str (:hp unit)
                     "/"
                     (:hp (meta unit))))
  (dom/set-text (dom/get-element :unit-fuel)
                (str (:fuel unit)
                     "/"
                     (:max-fuel-level (meta unit))))
  (dom/set-text (dom/get-element :unit-movement-type)
                (name (:movement-type (meta unit))))

  (dom/set-properties (dom/get-element :unit-details) {"style" "visibility:visible;"})


  (let [canvas (dom/get-element "unit-canvas")]
    (set! (.-width  canvas) game-drawer/+field-width+)
    (set! (.-height canvas) game-drawer/+field-height+)
    (game-drawer/draw-unit (.getContext canvas "2d")
                           @current-game
                           (aw-map/coord 0 0)
                           unit)))

(defn hide-unit-info []
  (dom/set-properties (dom/get-element :unit-details) {"style" "visibility:hidden;"}))


;;; Terrain Info

(defn show-terrain-info [terrain]
  (let [canvas (dom/get-element :terrain-canvas)
        context (.getContext canvas "2d")
        [tile-width tile-height] (:tile-size tiles/+terrain-tiles+)]
    (set! (.-width canvas) tile-width)
    (set! (.-height canvas) tile-height)
    (when (aw-map/is-building? terrain)
      (let [[terr color] terrain]
       (tile-drawer/draw-tile context
                              tiles/+terrain-tiles+
                              [:buildings terr color]
                              [game-drawer/+field-width+
                               (* 2 game-drawer/+field-height+)]
                              [0 0]
                              nil))))

  (dom/set-text (dom/get-element :terrain-name) (if (aw-map/is-building? terrain)
                                                  (let [[t c] terrain]
                                                    (str (name c) " " (name t)))
                                                  (name terrain))))


;;; Player info

(defn show-player-info [player]
  (dom/set-text (dom/get-element :player-name) (name (:color player)))
  (dom/set-text (dom/get-element :player-funds) (str (:funds player))))


;;; Internal utility functions

(defn ^:private sanitize-game
  "Function to remove dirty state the client code left. Examples
  are :current-path which isn't removed."
  [game]
  ;; All conditions in this cond MUST NOT match after their changes
  ;; were applied! Madness and terror awaits you when this isn't
  ;; provided.
  (cond
   (and (:current-path game)
        (or (:moved (aw-game/selected-unit game))
            (nil? (aw-game/selected-unit game)))) (recur (dissoc game :current-path))
   true game))


;;; Game drawing!

(defn draw-game
  "Generic game-drawing function. Strips non-important data used for
  drawing."
  [canvas game]
  (let [clean-game (-> game
                       (sanitize-game)
                       (dissoc :last-click-coord))
        last-click-coord (:last-click-coord game)]
    (if-let [unit (aw-game/selected-unit clean-game)]
      (show-unit-info unit)
      (hide-unit-info))

    (show-player-info (aw-game/current-player clean-game))

    (game-drawer/draw-game canvas
                           clean-game
                           last-click-coord)

    clean-game))

;;; Functions to handle user actions

(defn own-unit-clicked
  "Function ran when the players clicks on his own units."
  [game c unit]
  (cond
   ;; Bug: (= c null) => crash; (= null c) => false
   (= (aw-game/selected-coordinate game) c)
   (show-unit-action-menu game c unit)

   (and (nil? (aw-game/selected-unit game))
        (not (:moved unit)))
   (-> game
       (aw-game/select-unit c)
       (assoc :current-path (pathfinding/make-path c)))))

(defn enemy-unit-clicked
  "Function ran when the player clicks an enemy unit."
  [game c unit]
  (when-let [att-coord (aw-game/selected-coordinate game)]
    (when (aw-game/attack-possible? game att-coord c)
      (logging/log "Attack!")
      (show-attack-menu game c))))

(defn unit-clicked
  "Function ran when the player clicks on any unit. Dispatches to
  `own-unit-clicked' or `enemy-unit-clicked'."
  [game c]
  (let [unit (-> game :board (game-board/get-unit c))]
    (if (= (:color unit) (:color (aw-game/current-player game)))
      (own-unit-clicked game c unit)
      (enemy-unit-clicked game c unit))))

(defn terrain-clicked
  "Ran when the player clicks any terrain. Dispatches to functions
  handling buildings."
  [game c]
  (let [terrain (-> game :board (game-board/get-terrain c))]
    (cond
     (and (aw-game/selected-unit game)
          (> (-> game :current-path pathfinding/elements count) 1)
          (= c (-> game :current-path pathfinding/elements last))
          (not (game-board/get-unit (:board game) c)))
     ;; Okay. We do awesome stuff here: An action menu is shown after
     ;; a unit is moved. The problem is: There's the 'Cancel' option
     ;; which also reverts the move. The solution is kind of hacky: We
     ;; don't actually change the state of `current-game', we just
     ;; draw it. The global state is only changed AFTER the user
     ;; selected a continuing action (Wait, Capture, Attack, ...)
     (let [game-moved (aw-game/move-unit game (pathfinding/path->aw-path (:current-path game)))]
       (draw-game (dom/get-element "gameBoard") (dissoc game-moved :current-path))
       (show-unit-action-menu game-moved c (assoc (game-board/get-unit (:board game-moved) c)
                                             :moved true))
       nil))))

(defn clicked-on
  "Generic function ran when the player clicks on the game
  board. Dispatches between units and buildings."  [c]
  (when (and @current-game
             (not @current-action-menu)
             (aw-map/in-bounds? (-> @current-game :board :terrain) c))
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
        (reset! current-game (-> newgame
                                 (sanitize-game)
                                 (assoc :last-click-coord c)))))))

(defn mouse-moved
  "Function called when the mouse entered a new field on the game
  board."
  [c]
  (when-let [game @current-game]
    (let [unit (game-board/get-unit (:board game) c)
          terrain (game-board/get-terrain (:board game) c)
          current-unit (aw-game/selected-unit game)
          current-path (:current-path game)]

      (show-terrain-info terrain)

      ;; TODO: We need movement-range here
      (cond
       (and game
            current-unit
            current-path
            (not @current-action-menu))
       (when (pathfinding/update-path! current-path
                                       (aw-game/movement-range game)
                                       c
                                       (:board game)
                                       current-unit)
         ;; Hack: Redraw the game (there should be a fn for this)
         (reset! current-game game))

       ;; When we're on a field with an unit, show info about it
       unit (show-unit-info unit)

       ;; If there's no unit on the field, show info for selected unit
       current-unit (show-unit-info current-unit)

       ;; If there's neither a selected unit or an unit on the field,
       ;; hide the info
       (and (not unit)
            (not current-unit)) (hide-unit-info)))))

(let [last-coord (atom nil)]
 (defn ^:private mouse-moved-internal [event]
   (let [c (game-drawer/canvas->coord (aw-map/coord (.-offsetX event)
                                                    (.-offsetY event)))]
     (when-let [game @current-game]
       (when (and (aw-map/in-bounds? (-> game :board :terrain) c)
                  ;; CLJS-BUG: (= nil c) => Error (fixed in master)
                  (not= @last-coord c))
         (mouse-moved (reset! last-coord c)))))))

;;; Functions for setting up games in the DOM

(defn unregister-handlers [canvas]
  (gevents/removeAll canvas)
  (gevents/removeAll (dom/get-element :end-turn-button))
  (remove-watch current-game :redrawer))

(defn register-handlers [canvas]
  ;; remove all handlers prior adding new
  (unregister-handlers canvas)

  (event/listen canvas "click"
                (fn [event]
                  (clicked-on (game-drawer/canvas->coord
                               (aw-map/coord (.-offsetX event) (.-offsetY event)))))
                true)
  (event/listen canvas "mousemove" mouse-moved-internal)


  (event/listen (dom/get-element :end-turn-button) "click"
                (fn [event]
                  (when @current-game
                    (swap! current-game aw-game/next-player))))

  ;; We use add-watch to redraw the canvas every time the state changes
  (add-watch current-game :redrawer
             (fn [key ref old new]
               (draw-game canvas new))))

#_(register-handlers (dom/get-element :gameBoard))

#_(reset! current-game netwars.test-games/basic-game)

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

(defn ^:export start-game [game]
  (register-handlers (dom/get-element :gameBoard))
  (reset! current-game game))

(defn repl-connect []
  (repl/connect "http://localhost:9000/repl"))

(sorted-set [:foo] )