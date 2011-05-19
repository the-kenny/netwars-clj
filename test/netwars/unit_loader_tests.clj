(ns netwars.unit-loader-tests
  (:use [lazytest.describe :only [describe do-it it given testing using with]]
        [lazytest.context.stateful :only [stateful-fn-context]]
        [lazytest.expect :only [expect]]
        netwars.unit-loader
        [netwars.utilities :only [load-resource]]
        [clojure.set :only [subset?]]))

(describe load-units
  (it "loads an unit-spec from a file"
    (load-units (load-resource "units.xml")))
  (testing load-units!
    (it "loads an unit-spec into netwars.unit-loader/*unit-prototypes*" 
      (do (load-units! (load-resource "units.xml"))
          (= @*unit-prototypes* (load-units (load-resource "units.xml")))))))

(def unit-spec-context
     (stateful-fn-context #(load-units (load-resource "units.xml"))
                          (fn [_] nil)))

(describe "The loaded unit-spec"
  (using [unit-spec unit-spec-context]
    (it "is a map with > 0 entries"
      (and (map? @unit-spec)
           (> (count @unit-spec))))
    (testing "every unit-prototype in this spec"
      (given [required-keys #{:internal-name :name :hp}]
       (do-it "has the minimal required keys"
         (doseq [spec (vals @unit-spec)]
           (expect (subset? required-keys (-> spec keys set)))))
       (do-it "is accessible by its internal-name"
         (doseq [spec (vals @unit-spec)]
           (expect (= spec (get @unit-spec (:internal-name spec))))))))))
