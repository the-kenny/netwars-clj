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

            [netwars.menus.generic :as menu]
            [netwars.menus.unit-menu :as unit-menu]
            [netwars.menus.attack-menu :as attack-menu]
            [netwars.menus.factory-menu :as factory-menu]

            ;; [netwars.net.game-server :as game-server]

            ;; Load all crossovers prevent stripping
            [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.damagecalculator :as damage]
            [netwars.game-board :as game-board]
            [netwars.map-utils :as map-utils]
            [netwars.tile-drawer :as tile-drawer]

            [cljs.core.async :as async])
  (:use-macros [cljs.core.async.macros :only [go go-loop]]))

;;; TODO: Function for changing game-state instead of
;;; (reset current-game ...)
(def current-game-state  (atom []))
(def current-action-menu (atom nil))
(def last-click-coord    (atom nil))

(def game-move-channel
  "core.async channel receiving all irreversible game states."
  (async/chan))

;; (go-loop []
;;   (let [[val _] (alts! [game-move-channel])]
;;     (logging/log "channel event:" (count (aw-game/game-events val)))
;;     (recur)))

;; (def game-event-channel
;;   "core.async channel receiving all actions. Used for drawing."
;;   (async/sliding-buffer 10))

(defn game-states []
  @current-game-state)

(defn game-state
  ([states]
     (peek states))
  ([]
     (game-state (game-states))))

(defn pop-game-state! []
  (-> current-game-state
      (swap! #(if (seq (pop %))
                (pop %)
                (throw (js/Error. "Can't pop beyond current state."))))
      (game-state)))


(defn pop-all-game-states! []
  (swap! current-game-state (comp vector first)))

(defn push-game-state! [game]
  (-> current-game-state
   (swap! conj game)
   (game-state)))

(defn remove-stacked-game-states! []
  (swap! current-game-state (comp vector peek)))

(defn update-game-state!
  "Updates the game state irreversible."
  [f & args]
  (go (>! game-move-channel (game-state)))
  (swap! current-game-state (comp vector #(apply f % args) peek)))

(defn update-game-state-reversible! [f & args]
  (push-game-state! (apply f (game-state) args)))

(defn redraw-game! []
  (swap! current-game-state identity))

;;; Unit Actions (Attack, Wait, Capture, ...)

(defn action-cancel
  "Generic action for canceling actions.
   Deselects current unit and hides the action menu."
  []
  (pop-game-state!)
  (swap! current-action-menu menu/hide-menu))

(defn unit-action-wait
  "Deselects and 'Waits' the current unit."
  [game c]
  (update-game-state! aw-game/wait-unit)
  ;; TODO: Dismissal shouldn't be done in every action-fn
  (swap! current-action-menu menu/hide-menu))

(defn unit-action-attack
  "Functionally just hides the action menu. In the background this
  function applies the game-state given as argument because it after
  moving the current state is just drawn, not applied."
  [game c]
  (update-game-state-reversible! dissoc :current-path)
  (swap! current-action-menu menu/hide-menu))

(defn unit-action-capture
  "Action to capture buildings.
   Deselects current unit and hides action menu."
  [game c]
  {:pre (game-board/capture-possible? (:board game) c)}
  (update-game-state! #(-> game
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

(defn show-attack-menu [game c]
  (let [menu (attack-menu/attack-menu game c {;; :attack #(attack-action-attack game c)
                                              :cancel #(action-cancel)})]
    (menu/display-menu menu (dom/get-element :mapBox) (game-drawer/coord->canvas c))
    (reset! current-action-menu menu))
  ;; Return nil to indicate no re-draw is needed
  nil)

(defn buy-unit-action [game c unit]
  (assert (nil? (game-board/get-unit (:board game) c)))
  (update-game-state! aw-game/buy-unit c (:internal-name unit))
  (swap! current-action-menu menu/hide-menu))

(defn show-factory-menu [game c]
  (let [menu (factory-menu/factory-menu game c buy-unit-action)]
    (menu/display-menu menu (dom/get-element :mapBox) (game-drawer/coord->canvas c))
    (reset! current-action-menu menu))
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
                           (game-state)
                           (aw-map/coord 0 0)
                           unit)))

(defn show-attack-info [game target]
  (let [board (:board game)
        attacker (aw-game/selected-unit game)
        victim (game-board/get-unit (:board game) target)
        damage (damage/calculate-unrounded-damage
                (:damagetable game)
                [attacker (game-board/get-terrain board
                                                  (aw-game/selected-unit game))]
                [victim (game-board/get-terrain board target)])]
    (assert attacker)
    (assert victim)
    (.log js/console "Attack-Info:" attacker "vs." victim ":" damage)))

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
  [canvas game-states]
  (let [game (game-state game-states)
        clean-game (-> game
                       (sanitize-game))]
    (if-let [unit (aw-game/selected-unit clean-game)]
      (show-unit-info unit)
      (hide-unit-info))

    (show-player-info (aw-game/current-player clean-game))

    (game-drawer/draw-game canvas
                           clean-game
                           @last-click-coord)

    clean-game))

;;; Functions to handle user actions

(defn own-unit-clicked
  "Function ran when the players clicks on his own units."
  [game c unit right-click?]
  (cond
   ;; Bug: (= c null) => crash; (= null c) => false
   (= (aw-game/selected-coordinate game) c)
   (show-unit-action-menu game c unit)

   (and (nil? (aw-game/selected-unit game))
        (not (:moved unit)))
   (update-game-state-reversible!
    #(-> %
         (aw-game/select-unit c)
         (assoc :current-path (pathfinding/make-path c))))))

(defn enemy-unit-clicked
  "Function ran when the player clicks an enemy unit."
  [game c unit right-click?]
  (when-let [att-coord (aw-game/selected-coordinate game)]
    (when (aw-game/attack-possible? game att-coord c)
      (logging/log "Attack!")
      (update-game-state! #(let [att (aw-game/selected-coordinate %)
                                 def c]
                             (-> %
                                 ;; TODO: What's this?
                                 (dissoc :moving-disabled)
                                 (aw-game/perform-attack att def)
                                 (aw-game/deselect-unit)))))))

(defn unit-clicked
  "Function ran when the player clicks on any unit. Dispatches to
  `own-unit-clicked' or `enemy-unit-clicked'."
  [game c right-click?]
  (let [unit (-> game :board (game-board/get-unit c))]
    (if (= (:color unit) (:color (aw-game/current-player game)))
      (own-unit-clicked game c unit right-click?)
      (enemy-unit-clicked game c unit right-click?))))

(defn factory-clicked [game c]
  (let [[factory color] (game-board/get-terrain (:board game) c)]
   (when (= (:color (aw-game/current-player game)) color)
     (show-factory-menu game c))))

(defn terrain-clicked
  "Ran when the player clicks any terrain. Dispatches to functions
  handling buildings."
  [game c right-click?]
  (let [terrain (-> game :board (game-board/get-terrain c))]
    (if-not right-click?
      (cond
       ;; Move the unit
       (and (aw-game/selected-unit game)
            (when-let [path (:current-path game)]
              (> (-> path pathfinding/elements count) 1)
              (= c (-> path pathfinding/elements last)))
            (nil? (game-board/get-unit (:board game) c)))
       (update-game-state-reversible!
        (comp #(dissoc % :current-path) aw-game/move-unit)
        (pathfinding/path->aw-path (:current-path game)))

       ;; Show the factory menu
       (and (not (aw-game/selected-unit game))
            (aw-map/can-produce-units? terrain))
       (factory-clicked game c)

       ;; Undo the last move
       (> (count (game-states)) 1)
       (pop-game-state!))

      (when (> (count (game-states)) 1)
        (pop-game-state!)))))

(defn clicked-on
  "Generic function ran when the player clicks on the game
  board. Dispatches between units and buildings."
  [c right-click?]
  (when (and (game-state)
             (aw-map/in-bounds? (-> (game-state) :board :terrain) c))
    (reset! last-click-coord c)
    (if (and @current-action-menu
             (menu/toggle-menu? @current-action-menu))
      (do
        (swap! current-action-menu menu/hide-menu)
        (pop-game-state!))
      (let [game (game-state)
            board (:board game)
            terrain (game-board/get-terrain board c)
            unit (game-board/get-unit board c)]
        (cond
         unit    (unit-clicked    game c right-click?)
         terrain (terrain-clicked game c right-click?)
         true    nil)))))

(defn mouse-moved
  "Function called when the mouse entered a new field on the game
  board."
  [c]
  (when-let [game (game-state)]
    (let [unit (game-board/get-unit (:board game) c)
          terrain (game-board/get-terrain (:board game) c)
          current-unit (aw-game/selected-unit game)
          current-path (:current-path game)]

      (show-terrain-info terrain)

      (when
          (and game
               current-unit
               current-path
               (not @current-action-menu)
               (pathfinding/update-path! current-path
                                         (aw-game/movement-range game)
                                         c
                                         (:board game)
                                         current-unit))
        (redraw-game!))

      ;; TODO: We need movement-range here
      (cond
       ;; When the selected unit can attack the unit we're hovering
       ;; over, display attack info
       (and current-unit
            unit
            (not= (:color unit) (:color current-unit))
            (aw-game/attack-possible?
             game
             (aw-game/selected-coordinate game)
             c))
       (show-attack-info game c)

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
     (when-let [game (game-state)]
       (when (and (aw-map/in-bounds? (-> game :board :terrain) c)
                  ;; CLJS-BUG: (= nil c) => Error (fixed in master)
                  (not= @last-coord c))
         (mouse-moved (reset! last-coord c)))))))

;;; Functions for setting up games in the DOM

(defn unregister-handlers [canvas]
  (gevents/removeAll canvas)
  (gevents/removeAll (dom/get-element :end-turn-button))
  (remove-watch current-game-state :redrawer))

(defn register-handlers [canvas]
  ;; remove all handlers prior adding new
  (unregister-handlers canvas)

  (gevents/listen
   canvas (to-array
           [gevents/EventType.CLICK
            gevents/EventType.CONTEXTMENU])
   (fn [event]
     (.preventDefault event)
     (clicked-on (game-drawer/canvas->coord
                  (aw-map/coord (.-offsetX event) (.-offsetY event)))
                 (= gevents/BrowserEvent.MouseButton.RIGHT (.-button event)))))
  (gevents/listen canvas gevents/EventType.MOUSEMOVE mouse-moved-internal)
  (gevents/listen (dom/get-element :end-turn-button) gevents/EventType.CLICK
                  (fn [event]
                    (when (game-state)
                      (pop-all-game-states!)
                      (update-game-state! aw-game/next-player))))

  ;; We use add-watch to redraw the canvas every time the state changes
  (add-watch current-game-state :redrawer
             (fn [key ref old new]
               (logging/log "Game states:"
                            (pr-str (map (comp :type last aw-game/game-events) new)))
               (draw-game canvas new))))

(defn ^:export start-game [game]
  (register-handlers (dom/get-element :gameBoard))
  (update-game-state! (constantly (aw-game/start-game game))))

(defn ^:export start-game-from-server [map-name]
  (logging/log "Loading game...")
  (goog.net.XhrIo/send (str "/api/new-game/" map-name)
                       (fn [e]
                         (let [req (.-target e)
                               status (.getStatus req)
                               text (.getResponseText req)]
                          (logging/log "Got response")
                          (case status
                            200 (start-game (otw/decode-data text))
                            (js/alert (str "Error response from server: " text)))))))

(defn repl-connect []
  (repl/connect "http://localhost:9000/repl"))
