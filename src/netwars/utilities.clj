(ns netwars.utilities
  ;; (:use netwars.damagetable
  ;;       netwars.unit-loader
  ;;       vijual
  ;;       vijual.graphical)
  )

(defn load-resource
  "Create a stream to a resource in resources/"
  [name]
  (let [thr (Thread/currentThread)
        ldr (.getContextClassLoader thr)]
    (.getResourceAsStream ldr name)))

;;; Utilities to draw a graph with vijual
;; (defn- create-graph-structure
;; "Returns a list "
;;   [damagetable]
;;   (apply concat
;;          (map (fn [entry]
;;           (map vector (repeat (first entry)) (keys (second entry))))
;;         damagetable)))
