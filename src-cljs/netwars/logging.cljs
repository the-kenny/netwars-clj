(ns netwars.logging
  (:require [clojure.browser.dom :as dom]))

(defn log [& more]
  (.apply (.-log js/console) js/console
          (into-array (map #(if (satisfies? cljs.core.ISeqable %)
                              (pr-str %)
                              %)
                           more))))
