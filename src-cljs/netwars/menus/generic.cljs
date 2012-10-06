(ns netwars.menus.generic
  (:require [goog.ui.Menu :as gmenu]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [clojure.browser.event :as event]
            [netwars.logging :as logging]))

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

(defn make-toggle-menu [items]
  (let [menu (make-action-menu items)]
    (.setModel menu {:toggle true})
    menu))

(defn display-menu [menu parent position]
  (.setPosition menu (:x position) (:y position))
  (.render menu parent)
  menu)

(defn hide-menu [menu]
  (gdom/removeNode (.getElement menu))
  nil)

(defn toggle-menu? [menu]
  (and (.getModel menu)
       (:toggle (.getModel menu))))



;; (defn test-display-menu []
;;   (display-menu (dom/get-element :mapBox) nil [["Foo" #(logging/log "Foo")]
;;                                                ["Bar" #(logging/log "Bar")]
;;                                                ["Baz" #(logging/log "Baz")]]))
