(ns netwars.unit
  (:use [netwars.unit-loader :as loader]))

(defn- copy-entry [m key new-key]
  (assoc m new-key (get m key)))

(defn- copy-entry-in [m keys new-keys]
  (assoc-in m new-keys (get-in m keys)))

(defn- prepare-unit [type]
  (let [unit-prototype (get @loader/*unit-prototypes* type)]
    (-> unit-prototype
        (copy-entry :max-fuel-level :fuel-level)
        (copy-entry-in [:main-weapon :ammo] [:main-weapon :max-ammo])
        (copy-entry-in [:alt-weapon :ammo] [:alt-weapon :max-ammo]))))

(defn create-unit
  "Creates a unit with a specific color."
  ([type color]
     (assoc (prepare-unit type)
       :color color))
  ([type color & others]
     (apply assoc (create-unit type color) others)))

(defn low-fuel [u]
  (<= (/ (:fuel-level u) (:max-fuel-level u)) 0.5))

