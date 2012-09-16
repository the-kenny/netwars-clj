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
(def current-game-state (atom []))
(def current-action-menu (atom nil))

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

(defn push-game-state! [game]
  (-> current-game-state
   (swap! conj game)
   (game-state)))

(defn remove-stacked-game-states! []
  (swap! current-game-state (comp vector peek)))

(defn update-game-state! [f & args]
  #_(remove-stacked-game-states!)
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

(defn attack-action-attack [game c]
  (update-game-state! #(let [att (aw-game/selected-coordinate %)
                             def c]
                         (-> %
                             ;; TODO: What's this?
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
   (update-game-state-reversible!
    #(-> %
         (aw-game/select-unit c)
         (assoc :current-path (pathfinding/make-path c))))))

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

(defn factory-clicked [game c]
  (let [[factory color] (game-board/get-terrain (:board game) c)]
   (when (= (:color (aw-game/current-player game)) color)
     (show-factory-menu game c))))

(defn terrain-clicked
  "Ran when the player clicks any terrain. Dispatches to functions
  handling buildings."
  [game c]
  (let [terrain (-> game :board (game-board/get-terrain c))]
    (cond
     (and (aw-game/selected-unit game)
          (when-let [path (:current-path game)]
            (> (-> path pathfinding/elements count) 1)
            (= c (-> path pathfinding/elements last)))
          (nil? (game-board/get-unit (:board game) c)))
     ;; TODO: Somehow prevent drawing the movement-area
     (let [game (update-game-state-reversible!
                 (comp #(dissoc % :current-path) aw-game/move-unit)
                 (pathfinding/path->aw-path (:current-path game)))]
       (show-unit-action-menu
        game
        c
        (game-board/get-unit (:board game) c)))

     (and (not (aw-game/selected-unit game))
          (aw-map/can-produce-units? terrain))
     (factory-clicked game c)

     (> (count (game-states)) 1)
     (pop-game-state!))))

(defn clicked-on
  "Generic function ran when the player clicks on the game
  board. Dispatches between units and buildings."  [c]
  (when (and (game-state)
             (aw-map/in-bounds? (-> (game-state) :board :terrain) c))
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
         unit    (unit-clicked    game c)

         terrain (terrain-clicked game c)

         true nil)))))

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
         (redraw-game!))

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
         #_(logging/log "Mouse:" (pr-str c))
         (mouse-moved (reset! last-coord c)))))))

;;; Functions for setting up games in the DOM

(defn unregister-handlers [canvas]
  (gevents/removeAll canvas)
  (gevents/removeAll (dom/get-element :end-turn-button))
  (remove-watch current-game-state :redrawer))

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
                  (when (game-state)
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
                         (logging/log "Got data, started reading in")
                         (let [text (-> e .-target .getResponseText)]
                           (start-game (otw/decode-data text)))
                         (logging/log "Loaded game."))))

(defn repl-connect []
  (repl/connect "http://localhost:9000/repl"))
