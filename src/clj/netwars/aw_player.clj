(ns netwars.aw-player)

(defrecord AwPlayer [name color funds])

(defn make-player [name color funds]
  {:pre [(integer? funds)]}
  (AwPlayer. name color funds))

(defn is-player? [p]
  (instance? AwPlayer p))

(defn can-spend? [player amount]
  (>= (- (:funds player) amount) 0))

(defn spend-funds [player amount]
  {:pre [(can-spend? player amount)]
   :post [(>= (:funds %) 0)]}
  (update-in player [:funds] - amount))
