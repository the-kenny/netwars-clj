(ns netwars.player)

(defrecord AwPlayer [name color funds])

(defn make-player [name color funds]
  {:pre [(pos? funds)]}
  (AwPlayer. name color funds))

(defn can-spend? [player amount]
  (>= (- (:funds player) amount) 0))

(defn spend-funds [player amount]
  {:pre [(can-spend? player amount)]
   :post [(pos? (:funds %))]}
  (update-in player [:funds] - amount))
