(ns netwars.logging
  (:require [goog.dom :as dom]))

(defn log [& more]
  (.log js/console (apply str more)))

;;; TODO: Clear log when the game gets switched
(defn message [& more]
  (dom/appendChild (dom/getElement "messageLog")
                   (dom/createDom "div" nil (apply str more))))
