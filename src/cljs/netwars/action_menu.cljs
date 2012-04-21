(ns netwars.action-menu
  (:require [goog.ui.Menu :as gmenu]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [clojure.browser.event :as event]
            [netwars.logging :as logging]))

(defn display-menu [parent at items]
  (let [menu (goog.ui.Menu.)]
    (doseq [[item f] items
            :let [menu-item (goog.ui.MenuItem. item)]]
      (.addItem menu menu-item)
      (when f
        (event/listen menu-item "action" f)))
    (.render menu parent)
    menu))

(defn hide-menu [menu]
  (gdom/removeNode (.getElement menu)))

(defn test-display-menu []
  (display-menu (dom/get-element :mapBox) nil [["Foo" #(logging/log "Foo")]
                                               ["Bar" #(logging/log "Bar")]
                                               ["Baz" #(logging/log "Baz")]]))
