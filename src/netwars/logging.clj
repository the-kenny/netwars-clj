(ns netwars.logging)

(defn log [& more]
  (apply println more))
