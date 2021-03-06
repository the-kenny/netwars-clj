(ns netwars.aw-unit)

(defrecord AwUnit [internal-name color hp fuel])

(defn is-unit? [unit]
  (instance? AwUnit unit))

;;; Private methods for generating units

(defn- prepare-meta [prototype unit]
  (with-meta unit prototype))

(defn- prepare-main-weapon [prototype unit]
  (if-let [main (:main-weapon prototype)]
    (assoc-in unit [:weapons :main-weapon] (with-meta main main))
    unit))

(defn- prepare-alt-weapon [prototype unit]
  (if-let [alt (:alt-weapon prototype)]
    (assoc-in unit [:weapons :alt-weapon] (with-meta alt alt))
    unit))

(defn- prepare-loading [prototype unit]
  (if-let [limit (:transport-limit prototype)]
    (assoc unit :transport {:limit limit
                            :freight []})
    unit))

(def ^{:private true} fabrication-process [prepare-meta
                                           prepare-main-weapon
                                           prepare-alt-weapon
                                           prepare-loading])
(defn- prepare-unit [unit prototype]
  (let [partials (map #(partial % prototype) fabrication-process)]
    ((apply comp partials) unit)))


;;; Public functions

(def +movement-types+
  "All existing movement types."
  #{:foot
    :mechanical
    :tread
    :tires
    :fly
    :swim
    :transport
    :oozium
    :pipe
    :hover})

(defn find-prototypes [spec key value]
  (filter #(= (get % key) value) spec))

(defn find-prototype [spec key value]
  (first (find-prototypes spec key value)))

(defn make-unit [spec id-or-internal-name color]
  (when-let [prototype (or (find-prototype spec :id id-or-internal-name)
                           (find-prototype spec :internal-name id-or-internal-name))]
    (prepare-unit (AwUnit. (:internal-name prototype)
                           color
                           (:hp prototype)
                           (:max-fuel-level prototype))
                  prototype)))

(defn factory-units [spec factory]
  (map :internal-name (filter #(= (:factory %) factory) spec)))

;;; Misc Functions

(defn apply-damage [u d]
  (let [newu (update-in u [:hp] - d)]
    (when (pos? (:hp newu))
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
  (-> u :weapons :main-weapon))

(defn alt-weapon [u]
  (-> u :weapons :alt-weapon))

(defn weapons [u]
  (select-keys (:weapons u) [:main-weapon :alt-weapon]))

(defn has-weapons? [u]
  (and (contains? u :weapons) (not (empty? (:weapons u)))))

(defn weapon-available? [weapon]
  (not= 0 (:ammo weapon)))

(defn available-weapons [u]
  (into {} (filter (fn [[k v]] (weapon-available? v)) (weapons u))))

(defn ranged-weapon? [weapon]
  (not= 1 (reduce min (:range weapon))))

(defn fire-weapon
  "'Fires' a weapon (:alt-weapon or :main-weapon). Mainly uses ammo."
  [u main-or-alt]
  ;; {:pre [(weapon-available? ((get {:main-weapon main-weapon :alt-weapon alt-weapon}
  ;;                                 main-or-alt) u))]}
  (update-in u [:weapons main-or-alt :ammo] #(if (keyword? %) % (dec %))))

(defn low-ammo? [weapon]
  {:pre [(contains? (meta weapon) :ammo)]}
  (if (= (:ammo weapon) :infinity)
    false
    (< (:ammo weapon) (/ (:ammo (meta weapon)) 2))))

(defn can-capture? [unit]
  (contains? #{:infantry :mech} (:internal-name unit)))
