(ns netwars.logging
  (:require [goog.dom :as dom]))

(defn log [& more]
  (.log js/console (apply str more)))

(defn message [& more]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil (apply str more))))

(defn clear-messages []
  (dom/removeChildren (dom/getElement "messageLog")))
