(ns netwars.logging
  (:require [goog.dom :as dom]))

(defn log [& more]
  (.log js/console (apply str more)))

(defn message [& more]
  (let [messageLog (dom/getElement "messageLog")]
    (dom/appendChild messageLog
     (dom/createDom "div" nil (apply str more)))
    ;; Scroll to bottom found at:
    ;; http://www.ajax-community.de/javascript/6065-div-anspringen-timeout-berbr-cken.html
    (set! (. messageLog scrollTop) (.scrollHeight messageLog))))

(defn clear-messages []
  (dom/removeChildren (dom/getElement "messageLog")))
