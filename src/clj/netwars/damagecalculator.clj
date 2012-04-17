(ns netwars.damagecalculator
  (:require [netwars.aw-unit :as aw-unit]
            [netwars.aw-map :as aw-map]
            [clojure.set :as set]))

(defn get-damage [damagetable attacker defender]
  (get-in damagetable [attacker defender]))

(defn choose-weapon [damagetable attacker victim]
  (let [damage (get-damage damagetable
                           (:internal-name attacker)
                           (:internal-name victim))
        weapons (aw-unit/available-weapons attacker)
        comb (select-keys weapons (keys (set/rename-keys damage
                                                         {:alt-damage :alt-weapon
                                                          :damage :main-weapon})))]
    (cond
     (:main-weapon comb) {:main-weapon comb}
     (:alt-weapon comb) {:alt-weapon comb})))

;;	damage = floor(base-damage * (100 - (td2 * hp2)) * (hp1 / 10) * 100 / 10000);
(defn calculate-unrounded-damage [damagetable
                                  [attacker t1]
                                  [victim   t2]]
  (let [hp1 (:hp attacker)
        hp2 (:hp victim)
        ;; td1 (defense-value t1)
        td2 (aw-map/defense-value t2)
        [wt w] (first (choose-weapon damagetable attacker victim))
        damages (get-damage damagetable
                            (:internal-name attacker)
                            (:internal-name victim))
        base-damage (cond
                      (= wt :main-weapon) (:damage damages)
                      (= wt :alt-weapon)  (:alt-damage damages))]
    (Math/floor (/ (* base-damage (- 100 (* td2 hp2)) (/ hp1 10) 100) 10000))))

;;; TODO: Values in Advance Wars look more random
(defn round-damage [damage]
  (if (> (rem (Math/floor damage) 10) 0)
    (let [i (rem (Math/floor damage) 10)]
      (cond
       (> i 5) (* (Math/ceil (/ damage 10)) 10)
       (< i 5) (* (Math/floor (/ damage 10)) 10)
       (= i 5) (* ((rand-nth [#(Math/floor %) #(Math/ceil %)]) (/ damage 10)) 10)))
    damage))

(defn calculate-damage [damagetable
                        [attacker t1]
                        [victim   t2]]
  (Math/floor (/ (round-damage (calculate-unrounded-damage damagetable
                                                    [attacker t1]
                                                    [victim t2]))
          10)))
