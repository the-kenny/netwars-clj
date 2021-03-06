(ns netwars.test.aw-unit
  (:use netwars.aw-unit
        [clojure.java.io :only [resource]]
        [netwars.unit-loader :as loader]
        clojure.test))

(def ^:dynamic *spec* nil)

(defn- make-test-spec []
  (loader/load-units (resource "units.xml")))

(use-fixtures :each #(binding [*spec* (make-test-spec)]
                       (%)))

(deftest prototype-finding
  (is (= (:internal-name (find-prototype *spec* :id 0)) :infantry)
      "finds prototypes based on :id")
  (is (= (:id (find-prototype *spec* :internal-name :md-tank)) 1)
      "finds prototypes based on :internal-name")
  (is (nil? (find-prototype *spec* :id 999))
      "returns nil if it can't find a prototype")
  (is (nil? (find-prototype *spec*
                            :carries-towel ;; Today is towel-day
                            true))
      "returns nil if the key is unknown"))

(deftest test-make-unit
  (is (is-unit? (make-unit *spec* 0 :red)))
  (is (is-unit? (make-unit *spec* :infantry :red))))

(deftest test-unit-creation
  (let [weapon-unit (make-unit *spec* 0 :red)  ;Infantry
        transport-unit (make-unit *spec* 22 :green)]
    (is (instance? netwars.aw_unit.AwUnit weapon-unit))
    (is (= (:internal-name weapon-unit) :infantry))
    (is (= (:color weapon-unit :red)))
    (is (= (:hp weapon-unit 10)))
    (is (= (:fuel weapon-unit 99)))

    (is (= (meta weapon-unit)
           (find-prototype *spec* :internal-name (:internal-name weapon-unit)))
        "Instance contains prototype as meta")

    (testing "weapon properties"
      (is (contains? weapon-unit :weapons))
      (is (> (count (:weapons weapon-unit)) 0)))))

(deftest test-transporting
  (let [transporter (make-unit *spec* 22 :green)
        infantry (make-unit *spec* 0 :red)  ;Infantry
        ]
    (is (can-transport? transporter))
    (is (= #{:infantry :mech} (transport-types transporter)))

    (testing "loading"
      (is (contains? (transport-types transporter) (:internal-name infantry)))
      (let [loaded (transport-unit transporter infantry)]
        (is (= [] (-> transporter :transport :freight)))
        (is (= infantry (get-in loaded [:transport :freight 0])))))

    (testing "unloading"
      (let [loaded (transport-unit transporter infantry)]
        (is (= [transporter infantry] (unload-unit loaded 0)))
        (is (thrown? java.lang.Exception (unload-unit loaded -1)))
        (is (thrown? java.lang.Exception (unload-unit loaded 1)))
        (is (thrown? java.lang.Exception (unload-unit transporter 0)))))))

(deftest test-weapon-functions
  (let [infantry (make-unit *spec* 0 :red)
        megatank (make-unit *spec* 10 :red)
        apc (make-unit *spec* 22 :red)]
    ;; Infantry
    (is (has-weapons? infantry) "Infantry has a weapon")
    (is (= 1 (count (available-weapons infantry))) "One available weapon")
    (is (every? #{:main-weapon} (keys (weapons infantry))) "Only the main weapon")
    ;; Metagank
    (is (has-weapons? infantry) "Megatank has weapons")
    (is (= 2 (count (available-weapons megatank))) "Megatank has two weapons")
    (is (every? #{:main-weapon :alt-weapon} (keys (weapons megatank))) "main and alt")
    ;; APC
    (is (not (has-weapons? apc)) "An APC doesn't have weapons")
    (is (empty? (weapons apc)) "weapons returns an empty seq")))

(deftest test-ranged-weapon?
  (let [infantry (main-weapon (make-unit *spec* :infantry :red))
        artillery (main-weapon (make-unit *spec* :artillery :red))]
    (is (not (ranged-weapon? infantry)))
    (is (ranged-weapon? artillery))))

(deftest test-fire-weapon
  (let [infantry (make-unit *spec* :infantry :red)
        tank (make-unit *spec* :tank :red)]
    (is (= :infinity(-> infantry (fire-weapon :main-weapon) main-weapon :ammo)))
    (is (= (dec (-> tank main-weapon :ammo))
           (-> tank (fire-weapon :main-weapon) main-weapon :ammo)))))

(defn- deplete-ammo
  "Helper function which returns u with all ammunition in alt- or main-weapon removed"
  [u main-or-alt]
  (assoc-in u [:weapons main-or-alt :ammo] 0))

(deftest test-deplete-ammo
  (is (zero? (-> (make-unit *spec* :infantry :red)
                 (deplete-ammo :main-weapon)
                 main-weapon
                 :ammo))))

(deftest test-available-weapons
  (let [infantry (make-unit *spec* 0 :red)
        megatank (make-unit *spec* 10 :red)
        apc (make-unit *spec* 22 :red)]
    (is (every? #{:main-weapon} (keys (available-weapons infantry))))
    (is (every? #{:main-weapon :alt-weapon} (keys (available-weapons megatank))))
    (is (empty? (available-weapons apc)))

    (is (empty? (-> infantry (deplete-ammo :main-weapon) available-weapons)))
    
    (is (every? #{:main-weapon} (keys (-> megatank (deplete-ammo :alt-weapon) available-weapons))) )))

(deftest test-can-capture?
  (is (can-capture? (make-unit *spec* 0  :red))) ;Infantry
  (is (can-capture? (make-unit *spec* 20 :red))) ;Mech
  (is (not (can-capture? (make-unit *spec* 22 :red)))))
