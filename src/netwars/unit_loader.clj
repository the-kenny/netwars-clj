(ns netwars.unit-loader
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip])
  (:use [clojure.set :only [rename-keys]]))

(def *unit-prototypes* (atom nil))

(def #^{:private true
        :doc "A map of maps to represent the renaming of the xml-keywords."}
     +tag-mappings+
     {:unit {:internal_name :internal-name}
      :dummy_unit {:internal_name :internal-name}
      :movement {:range :movement-range
                 :type :movement-type
                 :capture_buildings :can-capture}
      :fuel {:level :max-fuel-level
             :waste :fuel-waste
             :remove_without :remove-without-fuel}
      :combat {:counterattack :can-counterattack
               :knock_down :can-knock-down-units
               :damage_multiplier :damage-multiplier ;Used in dummy_units
               }
      ;; Supply
      :supply {:repair_points :repair-points}
      :environment {:type :repair-types}
      ;; Transport
      :transport {:count :transport-limit}
      :transportable {:type :transport-types}})


(defmulti parse-element :tag)


(defmacro #^{:private true} def-simple-replace-parse [element]
  `(defmethod parse-element ~element
     [{attrs# :attrs}]
     (rename-keys attrs# (get +tag-mappings+ ~element {}))))

(defmacro #^{:private true} def-recursive-replace-parse [element]
  `(defmethod parse-element ~element
     [{attrs# :attrs content# :content}]
     (apply merge (rename-keys attrs# (get +tag-mappings+ ~element {}))
         (map parse-element content#))))

(def-recursive-replace-parse :unit)
(def-recursive-replace-parse :dummy_unit)

(def-simple-replace-parse :fabrication)
(def-simple-replace-parse :movement)
(def-simple-replace-parse :fuel)

;;; Weapon-Specific Methods
(def-recursive-replace-parse :combat)

(defmethod parse-element :main_weapon
  [{:keys [attrs]}]
  {:main-weapon attrs})

(defmethod parse-element :alt_weapon
  [{:keys [attrs]}]
  (:alt-weapon attrs))

(defmethod parse-element :explosive_charge
  [{attrs :attrs}]
  {:explosive-charge attrs})

;;; Supply
(defmethod parse-element :supply
  [{:keys [attrs content]}]
  (apply merge (if attrs                ;Small hack to handle attrs = nil
                 (rename-keys attrs (get +tag-mappings+ :supply {}))
                 {})
         {:repair-types (set (map parse-element content))}))

(defmethod parse-element :environment
  [{attrs :attrs}]
  (keyword (:type attrs)))

(defmethod parse-element :available
  [{attrs :attrs}]
  (keyword (:type attrs)))

;;; Transport
(defmethod parse-element :transport
  [{:keys [attrs content]}]
  (apply merge (rename-keys attrs (get +tag-mappings+ :transport {}))
         {:transportable (set (map parse-element content))}))

(defmethod parse-element :transportable
  [{attrs :attrs}]
  (keyword (:type attrs)))


(defn parse-units [xml-tree]
  (map parse-element (:content xml-tree)))

(defn load-units [stream]
  "Load and returns a list of units.
 stream is a stream pointing to the xml-file.."
  (parse-units (xml/parse stream)))

(defn load-units! [stream]
  "Loads units and stores them in *unit-prototypes*"
  (reset! *unit-prototypes* (load-units stream)))
