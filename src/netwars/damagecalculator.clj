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
