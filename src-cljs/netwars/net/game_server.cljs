(ns netwars.net.game-server
  (:require [netwars.aw-game :as aw-game]
            [netwars.logging :as logging]
            [cljs.core.async :as async])
  (:use-macros [cljs.core.async.macros :only [go go-loop]]))

(defn ^:private game-server-event-count [game-uuid]
  ;; TODO: Stub
  0)

(defn ^:private game-received [game-uuid last-stored-event game]
  (let [to-send (- (count (aw-game/game-events game))
                   last-stored-event)]
    (assert (>= to-send 0))
    (when (pos? to-send)
      (let [events (take-last to-send (aw-game/game-events game))]
        (logging/log (str "Sending " (count events)  " events."))
        (+ last-stored-event to-send)))))

(defn save-game-channel [game-uuid channel]
  ;; - Get event-number from server
  ;; - Compare with actual game event count
  ;; - Send missing events on every channel entry
  (go-loop [last-stored-event (game-server-event-count game-uuid)]
    (let [[val _] (alts! [channel])
          new-event-number (game-received game-uuid last-stored-event val)]
      (logging/log "stored events up to" new-event-number)
      (recur new-event-number))))
