(ns netwars.aw-unit
  (:use [netwars.unit-loader :as loader]))

(defrecord AwUnit [internal-name color hp fuel])

;;; Private methods for generating units

(def ^{:private true} fabrication-process (atom []))
(defn- prepare-unit [unit prototype]
  (let [partials (map #(partial % prototype) @fabrication-process)]
    ((apply comp partials) unit)))

(defn- prepare-weapons [prototype unit]
  (if-let [main (:main-weapon prototype)]
   (assoc unit :weapons (if-let [alt (:alt-weapon prototype)] [main alt] [main]))
   unit))
(swap! fabrication-process conj #'prepare-weapons)


(defn- prepare-loading [prototype unit]
  (if-let [limit (:transport-limit prototype)]
    (assoc unit :transport {:limit limit
                            :freight []})
    unit))
(swap! fabrication-process conj #'prepare-loading)


;;; Public methods

(defn make-unit [spec id color]
  (when-let [prototype (loader/find-prototype spec :id id)]
    (with-meta (-> (AwUnit. (:internal-name prototype)
                             color
                             (:hp prototype)
                             (:max-fuel-level prototype))
                   (prepare-unit prototype))
      prototype)))

;;; Transport Methods

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

(comment
  (def +spec+ (loader/load-units
               "/Users/moritz/Development/netwars-clj/resources/units.xml")))
