(ns netwars.utilities)

(defn load-resource
  "Create a stream to a resource in resources/"
  [name]
  (let [thr (Thread/currentThread)
        ldr (.getContextClassLoader thr)]
    (.getResourceAsStream ldr name)))
