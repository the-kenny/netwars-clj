(ns netwars.map-utils
  (:use netwars.map-loader
        [netwars.aw-map :only [at]])
  (:require clojure.inspector))

;;; TODO: Simplify
(defn neighbours [terrain c]
  (let [msta (partial at terrain)
        x (:x c) y (:y c)]
   (hash-map
    :north (msta x (dec y))
    :east (msta (inc x) y)
    :south (msta x (inc y))
    :west (msta (dec x) y)

    :north-east (msta (inc x) (dec y))
    :south-east (msta (inc x) (inc y))
    :north-west (msta (dec x) (dec y))
    :south-west (msta (dec x) (inc y)))))

(defn rectangular-direction [dir]
  (get {:north [:east :west]
        :south [:east :west]
        :east [:north :south]
        :west [:north :south]} dir nil))

(defn drop-neighbours-behind [direction nbs]
  (select-keys nbs
               (condp = direction
                 :north [:north
                         :north-west :north-east
                         :east :west]
                 :east [:east
                        :north-east :south-east
                        :north :south]
                 :west [:west
                        :north-west :north-west
                        :north :south]
                 :south [:south
                         :south-west :south-east
                         :east :west])))

(comment
 (defn inspect-terrain [loaded-map]
   (clojure.inspector/inspect
    (apply merge (for [x (range 30) y (range 20)]
                   {[x y] (at terrain (coord x y))})))))

;; (defn is-ground [terrain]
;;   (boolean (and terrain
;;                 (not (#{:water :reef :bridge :beach} terrain)))))

(def ^{:private true}
     +not-traversable-types+
     {:foot      #{:pipe :reef :water}
      :mech      #{:pipe :reef :water}
      :tread     #{:mountain :pipe :reef :water :river}
      :tires     #{:mountain :pipe :reef :water :river}
      :air       #{:pipe}
      :sea       #{:plain :forest :building :bridge :hq
                   :mountain :pipe :street :silo :river :beach}
      :transport #{:plain :forest :building :bridge :hq
                   :mointain :pipe :street :silo :river}
      :oozium    #{:pipe :reef :water}})

(defn can-pass? [move-type terrain]
  (when-let [npt (move-type +not-traversable-types+)]
    (not (boolean (npt terrain)))))

(def ^{:private true} +movement-costs+
     ;; movement-costs = 1 and not-traversable were omitted
     {:plain {:tires 2}
      :forest {:tread 2
               :tires 3}
      :mountain {:foot 2}
      :reef {:sea 2
             :transport 2}
      :river {:foot 2}})

(defn movement-costs [terrain movement-type]
  (when (can-pass? movement-type terrain)
    (get-in +movement-costs+ [terrain movement-type] 1)))
