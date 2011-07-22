(ns netwars.damagecalculator
  (:use [netwars.aw-unit :as unit]
        [netwars.damagetable :as damagetable]
        [clojure.set :only [rename-keys]]))

(defn defense-value [terrain]
  (let [[t c] (if (keyword? terrain) [terrain] terrain)]
    (case t
      (:plain :reef) 1
      :forest 2
      (:city :base :airport :port :lab) 3
      (:headquarter :mountain) (if (= c :white) 3 4)
      0)))

(defn choose-weapon [damagetable attacker victim]
  (let [damage (damagetable/get-damage damagetable
                                       (:internal-name attacker)
                                       (:internal-name victim))
        weapons (available-weapons attacker)
        comb (select-keys weapons (keys (rename-keys damage
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
        td2 (defense-value t2)
        [wt w] (first (choose-weapon damagetable attacker victim))
        damages (damagetable/get-damage damagetable
                                        (:internal-name attacker)
                                        (:internal-name victim))
        base-damage (case wt
                      :main-weapon (:damage damages)
                      :alt-weapon (:alt-damage damages))]
    (Math/floor (/ (* base-damage (- 100 (* td2 hp2)) (/ hp1 10) 100) 10000))))

(defn round-damage [damage]
  (if (> (rem (int damage) 10) 0)
    (let [i (rem (int damage) 10)]
      (cond
       (> i 5) (* (Math/ceil (/ damage 10)) 10)
       (< i 5) (* (Math/floor (/ damage 10)) 10)
       (= i 5) (* ((rand-nth [#(Math/floor %) #(Math/ceil %)]) (/ damage 10)) 10)))
    damage))

(defn attack-unit [damagetable
                   [attacker t1]
                   [victim   t2]]
  (let [newvic (update-in victim [:hp] -
                      (int (/ (round-damage (calculate-unrounded-damage damagetable
                                                                    [attacker t1]
                                                                    [victim t2]))
                          10)))
        newatt (update-in attacker [:hp] -
                          (int (/ (round-damage (calculate-unrounded-damage damagetable
                                                                        [newvic t2]
                                                                        [attacker t1]))
                              10)))]
    [(if (<= (:hp newatt) 0) nil newatt)
     (if (<= (:hp newvic) 0) nil newvic)]))
