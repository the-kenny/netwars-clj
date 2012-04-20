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

            ;; Load all crossovers prevent stripping
            [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.aw-unit :as aw-unit]
            [netwars.damagecalculator :as damagecalculator]
            [netwars.game-board :as game-board]
            [netwars.map-utils :as map-utils]
            [netwars.tile-drawer :as tile-drawer]))

(def current-game (atom nil))

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
