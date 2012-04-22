(ns netwars.action-menu
  (:require [goog.ui.Menu :as gmenu]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [clojure.browser.event :as event]
            [netwars.logging :as logging]

            [netwars.aw-game :as aw-game]
            [netwars.game-board :as game-board]
            [netwars.aw-map :as aw-map]))

(defn make-action-menu [items]
  (let [menu (goog.ui.Menu.)]
    (doseq [[item f disabled?] items
            :let [menu-item (if-not (= item :separator)
                              (goog.ui.MenuItem. item)
                              (goog.ui.MenuSeparator.))]]
      (.addItem menu menu-item)
      (when f
        (event/listen menu-item "action" f))
      (when disabled? (.setEnabled menu-item false)))
    menu))

(defn display-menu [menu parent position]
  (.setPosition menu
                (+ (:x position) 20)
                (+ (:y position) 20))
  (.render menu parent)
  menu)

(defn hide-menu [menu]
  (gdom/removeNode (.getElement menu))
  nil)


(defn unit-action-menu [game pos fns]
  (let [board (:board game)
        unit (aw-game/selected-unit game)
        capture? (game-board/capture-possible? board pos)]
    (make-action-menu [["Capture" (:capture fns) (not capture?)]
                       ["Attack"  (:attack fns)  :disabled]
                       [:separator]
                       ["Wait"    (:wait fns)]])))

;; (defn test-display-menu []
;;   (display-menu (dom/get-element :mapBox) nil [["Foo" #(logging/log "Foo")]
;;                                                ["Bar" #(logging/log "Bar")]
;;                                                ["Baz" #(logging/log "Baz")]]))
