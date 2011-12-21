(ns netwars.aw-unit)

(defrecord AwUnit [internal-name color hp fuel])

;;; Private methods for generating units

(def ^{:private true} fabrication-process (atom []))
(defn- prepare-unit [unit prototype]
  (let [partials (map #(partial % prototype) @fabrication-process)]
    ((apply comp partials) unit)))

(defn- prepare-main-weapon [prototype unit]
  (if-let [main (:main-weapon prototype)]
    (update-in unit [:weapons] conj (with-meta main main))
    unit))
(swap! fabrication-process conj #'prepare-main-weapon)

(defn- prepare-alt-weapon [prototype unit]
  (if-let [alt (:alt-weapon prototype)]
    (update-in unit [:weapons] conj (with-meta alt alt))
    unit))
(swap! fabrication-process conj #'prepare-alt-weapon)


(defn- prepare-loading [prototype unit]
  (if-let [limit (:transport-limit prototype)]
    (assoc unit :transport {:limit limit
                            :freight []})
    unit))
(swap! fabrication-process conj #'prepare-loading)

(defn- find-prototype [spec key value]
  (first (filter #(= (get % key) value) spec)))

;;; TODO: Make it possible to create units by internal-name
(defn make-unit [spec id color]
  (when-let [prototype (find-prototype spec :id id)]
    (with-meta (-> (AwUnit. (:internal-name prototype)
                             color
                             (:hp prototype)
                             (:max-fuel-level prototype))
                   (prepare-unit prototype))
      prototype)))

;;; Misc Functions

(defn apply-damage [u d]
  (let [newu (update-in u [:hp] - d)]
    (when (> (:hp newu) 0)
      newu)))

;;; Transport Functions

(defn can-transport? [u]
  (contains? u :transport))

(defn transport-types [u]
  {:pre [(meta u) (can-transport? u)]}
  (:transportable (meta u)))

(defn transport-unit [transporter u]
  {:pre [(can-transport? transporter)
         (contains? (transport-types transporter) (:internal-name u))]}
  (update-in transporter [:transport :freight] conj u))

(defn unload-unit
  "Unloads an unit based on index. Returns [transporter, unloaded-unit]"
  [transporter i]
  {:pre [(can-transport? transporter)]
   :post [(= 2 (count %))]}
  (when-not (< -1 i (count (-> transporter :transport :freight)))
    (throw (java.lang.Exception. (str "Can't unload unit at index " i))))
  (let [u (get-in transporter [:transport :freight i])
        newlst (remove #{u} (-> transporter :transport :freight))]
    [(assoc-in transporter [:transport :freight] newlst) u]))

;;; Weapon methods

(defn main-weapon [u]
  (first (:weapons u)))

(defn alt-weapon [u]
  (second (:weapons u)))

(defn weapons [u]
  (when-let [m {:main-weapon (main-weapon u)}]
    (if-let [alt (alt-weapon u)]
      (assoc m :alt-weapon alt)
      m)))

(defn has-weapons? [u]
  (contains? u :weapons))

(defn weapon-available? [weapon]
  (not= 0 (:ammo weapon)))

(defn available-weapons [u]
  (into {} (filter #(weapon-available? (val %)) (weapons u))))

(defn low-ammo? [weapon]
  {:pre [(contains? (meta weapon) :ammo)]}
  (if (= (:ammo weapon) :infinity)
    false
    (< (:ammo weapon) (/ (:ammo (meta weapon)) 2))))
