(ns netwars.unit-info
  (:require [goog.dom :as dom]
            [netwars.connection :as connection]
            [netwars.logging :as logging]))

(defn show-unit-info [unit]
  (logging/message-html (str  "HP: " (:hp unit) "/" (:hp (meta unit))
                              " Fuel: " (:fuel unit) "/" (:max-fuel-level (meta unit)))))

(defn request-unit-info [c]
  (connection/send-data {:type :unit-info
                         :coordinate c}))

(defmethod connection/handle-response :unit-info [message]
  (show-unit-info (:unit message)))
