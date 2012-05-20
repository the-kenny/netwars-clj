(ns netwars.logging
  (:require [clojure.browser.dom :as dom]))

(defn log [& more]
  (.log js/console (apply pr-str more)))
