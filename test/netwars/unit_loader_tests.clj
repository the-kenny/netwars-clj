(ns netwars.unit-loader-tests
  (:use [lazytest.describe :only [describe do-it it given testing using with]]
        [lazytest.context.stateful :only [stateful-fn-context]]
        [lazytest.expect :only [expect]]
        netwars.unit-loader
        [netwars.utilities :only [load-resource]]
        [clojure.set :only [subset?]]))

(describe load-units
  (it "loads an unit-spec from a file"
    (load-units (load-resource "units.xml"))))

(def unit-spec-context
     (stateful-fn-context #(load-units (load-resource "units.xml"))
                          (fn [_] nil)))

(describe "The loaded unit-spec"
  (using [unit-spec unit-spec-context]
    (it "is a sequence with >0 elements"
      (and (seq? @unit-spec)
           (> (count @unit-spec))))

    (testing "every unit-prototype in this spec"
      (given [required-keys #{:internal-name :name :hp}]
        (do-it "is a map"
          (doseq [spec  @unit-spec]
           (expect (map? spec))))
        
        (do-it "has the minimal required keys"
         (doseq [spec @unit-spec]
           (expect (subset? required-keys (-> spec keys set)))))))))

(describe find-prototype
  (given [spec (load-units (load-resource "units.xml"))]
    (it "finds prototypes based on :id"
      (= (:internal-name (find-prototype spec :id 0)) :infantry))
    (it "finds prototypes based on :internal-name"
      (= (:id (find-prototype spec :internal-name :md-tank)) 1))
    (it "returns nil if it can't find a prototype"
      (nil? (find-prototype spec :id 999)))
    (it "returns nil if the key is unknown"
      (nil? (find-prototype spec
                            :carries-towel ;; Today is towel-day
                            true))))) 
