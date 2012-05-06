(ns netwars.damagetable
  (:require [clojure.xml :as xml]
            [clojure.set :as set]))

(defn- mapvals
  "Maps f over the values in a map"
  [f m]
  (zipmap (keys m) (map f (vals m))))

(defmulti #^{:private true} parse-element :tag)

(defmethod #^{:private true} parse-element :unit
  [{:keys [attrs content]}]
  {(keyword (:name attrs)) (apply merge (map parse-element content))})

(defmethod #^{:private true} parse-element :enemy
  [{attrs :attrs}]
  {(keyword (:name attrs))
   (into {} (remove (fn [[_ v]] (= v -1))
            (mapvals #(when % (read-string %))
                     (set/rename-keys
                      (select-keys attrs [:damage :alt_damage])
                      {:alt_damage :alt-damage}))))})

(defn- parse-damagetable [xml-tree]
  (apply merge (map parse-element (:content xml-tree))))


(defn load-damagetable [source]
  (parse-damagetable (xml/parse source)))
