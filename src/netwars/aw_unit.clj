(ns netwars.aw-unit
  (:use [netwars.unit-loader :as loader]))

(defrecord AwUnit [internal-name color hp fuel])

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

(defn make-unit [spec id color]
  (when-let [prototype (loader/find-prototype spec :id id)]
    (with-meta (-> (AwUnit. (:internal-name prototype)
                             color
                             (:hp prototype)
                             (:max-fuel-level prototype))
                   (prepare-unit prototype))
      prototype)))


(comment
  (def +spec+ (loader/load-units
               "/Users/moritz/Development/netwars-clj/resources/units.xml")))
