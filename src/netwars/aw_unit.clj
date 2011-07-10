(ns netwars.aw-unit
  (:use [netwars.unit-loader :as loader]))

(defrecord AwUnit [internal-name color hp fuel])

(defn prepare-weapons [prototype unit]
  (if-let [main (:main-weapon prototype)]
   (assoc unit :weapons (if-let [alt (:alt-weapon prototype)] [main alt] [main]))
   unit))


(def ^{:private true} *unit-fabrication-process*
     [prepare-weapons])

(defn- prepare-unit [unit prototype]
  ((apply comp *unit-fabrication-process*) prototype unit))

(defn make-unit [spec id color]
  (when-let [prototype (loader/find-prototype spec :id id)]
    (with-meta (-> (AwUnit. (:internal-name prototype)
                             color
                             (:hp prototype)
                             (:max-fuel-level prototype))
                   (prepare-unit prototype))
      prototype)))
