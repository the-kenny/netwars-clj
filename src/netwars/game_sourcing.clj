(ns netwars.game-sourcing
  (:require [netwars.aw-game :as aw-game]
            [netwars.aw-map :as aw-map]
            [netwars.game-creator :as game-creator]
            [netwars.game-board :as board]))

(defmulti apply-game-event (fn [game event] (:type event)))

(defmacro handle-game-event [type [game [& parameters] & [as event]] & body]
  (let [event* (gensym "event")]
   `(defmethod apply-game-event ~type
      [~game ~event*]
      (let ~(if (and (= :as as) event)
              `[{:keys ~(vec parameters) :as ~event} ~event*]
              `[{:keys ~(vec parameters)} ~event*])
        (let [ret# (do ~@body)]
          (assert (= ~event* (-> ret# (aw-game/game-events) last))) 
          ret#)))))

(defmethod apply-game-event :game-started
  [game {:keys [settings unit-spec damagetable initial-board players]}]
  (assert (nil? game))
  (-> (game-creator/make-game :game-board initial-board
                              :unit-spec unit-spec
                              :damagetable damagetable
                              :settings settings
                              :players players
                              ;; TODO: handle info
                              )
      (aw-game/start-game)))

(handle-game-event :turn-completed
  [game [player]]
  (let [game* (aw-game/next-player game)]
    (assert (= player (aw-game/current-player game*)))
    game*))

(handle-game-event :unit-moved
  [game [from to path fuel-costs]]
  (let [game* (-> game
                  (aw-game/select-unit from)
                  (aw-game/move-unit path))]
    (assert (= fuel-costs
               (- (:fuel (board/get-unit (:board game) from))
                  (:fuel (board/get-unit (:board game*) to)))))
    game*))

(handle-game-event :building-captured
  [game [building coordinate unit]]
  (let [game* (-> game
                  (aw-game/select-unit coordinate)
                  (aw-game/capture-building coordinate))
        old-building (board/get-terrain (:board game) coordinate)]
    (assert (= (aw-map/capture-points old-building)
               (+ (aw-map/capture-points building)
                  (:hp unit))))
    game*))

(handle-game-event :bought-unit
  [game [unit coordinate price]]
  (let [game* (aw-game/buy-unit game
                                coordinate
                                (:internal-name unit))]
    (assert (= (assoc unit :moved true)
               (board/get-unit (:board game*) coordinate)))
    game*))

(handle-game-event :attack
  [game [from attacker to victim from damage]]
  (let [game* (-> game
                  (aw-game/perform-attack from to
                                          :damage damage
                                          :disable-counterattack? true))]
    (assert (= (board/get-unit (:board game*) to)
               victim))
    (assert (= (board/get-unit (:board game*) from)
               (assoc attacker
                 :moved true)))
    game*))

(handle-game-event :counter-attack
  [game [from attacker to victim from damage]]
  (let [game* (-> game
                  (aw-game/perform-attack from to
                                          :damage damage
                                          :counterattack true))]
    (assert (= (board/get-unit (:board game*) to)
               victim))
    (assert (= (board/get-unit (:board game*) from)
               attacker))
    game*))


;; Local Variables:
;; eval: (put-clojure-indent (quote handle-game-event) (quote defun))
;; End:
