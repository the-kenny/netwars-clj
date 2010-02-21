(ns netwars.map-loader
  (:use [clojure.contrib.def :only [defalias]]
        netwars.unit-loader)
  (:require [clojure.contrib.str-utils2 :as str2])
  (:import java.nio.ByteBuffer))

(defstruct map-file
  :filename
  :name
  :author
  :description
  :type
  :tileset
  :terrain-data                         ;Top-to-bottom, left-to-right
  :unit-data)


(def #^{:private true} tilesets
     [:normal :snow :desert :wasteland :aw1 :aw2])

(def #^{:private true} terrain-values
	 {0   :plain
	  60  :water
	  3   :river
	  1   :street
	  32  :bridge
	  2   :bridge
	  16  :pipe
	  226 :segment-pipe
	  167 :wreckage
	  150 :mountain
	  90  :forest
	  30  :reef
	  39  :beach})

(def #^{:private true} terrain-building-values
     [:headquarter :city :base :airport :port :tower :lab])

(def #^{:private true} terrain-color-values
	 [:red :blue :green :yellow :black :white])

(defalias unit-color-values terrain-color-values)

(defmacro read-when-possible [buf & body]
  `(when (.hasRemaining ~buf)
     ~@body))

(defn read-binary-file [#^String uri]
  (let [file (java.io.File. uri)
		buf (java.nio.ByteBuffer/allocate (.length file))]
	(.. (java.io.FileInputStream. file) (getChannel) (read buf))
	(.rewind buf)))

(defn read-n-string [buf len]
  (if (> len 0)
   (let [arr (make-array Byte/TYPE len)]
     (.get buf arr)
     (apply str (map char  arr)))
   ""))

(defn read-null-string [buf]
  (loop [s ""]
	(if-let [c #(let [c (.getChar buf)] (if (= % 0) nil %))]
	  (recur (str s c))
	  s)))

(defn read-byte [#^java.nio.ByteBuffer buf]
  (.get buf))

(defn read-dword [#^java.nio.ByteBuffer buf]
  (.getShort buf))

(defn read-int32 [#^java.nio.ByteBuffer buf]
  (.getInt buf))

(defn parse-tileset [byte]
  (get tilesets byte))

(defn parse-terrain [value]
  (cond
   (<= 0 value 299)
   (get terrain-values (+ (rem value 30)
                          (* 30 (int (/ value 30)))))
   (<= 300 value 499)
   (when-let [terr (get terrain-building-values (rem (- value 300) 10))]
     [terr (get terrain-color-values (int (/ (- value 300) 10)))])
   ;; (<= 900 value 1299) ni
   ;; (+ 900 (rem (- value 900) 20)
   ;;    (* 20 (int (/ (- value 900) 20))))
   ))

(defn parse-terrain-data [data]
  (map #(if-let [ret (parse-terrain %)]
          ret
          :unimplemented)
       data))

(defn find-unit-by-id [id]
  (when @*unit-prototypes*
    (first (filter #(= (get % :id) (str id)) @*unit-prototypes*))))

(defn parse-unit-data [data width height]
  (if @*unit-prototypes*
    (into {}
          (for [x (range width) y (range height)
                :let [val (nth data (+ (* x height) y))
                      id (rem (- val 500) 40)
                      color (get unit-color-values 
                                 (int (/ (- val 500) 40)))]
                :when (not= val -1)]
            [[x y] [(:internal-name (find-unit-by-id id)) color]]))
    :units-not-loaded)) 

(def aws-spec '[[:editor-version 6 :string]
				[:format-version 3 :string]
				[nil 1 :byte]
				[:width 1 :byte]
				[:height 1 :byte]
				[:tileset 1 :byte parse-tileset]
				[:terrain-data (* :width :height 2) :dword parse-terrain-data]
				[:unit-data (* :width :height 2) :dword parse-unit-data]])

(defn load-map [file] 
  (let [ ;; I hope Little-Endianess doesn't cause problems
        buf (.order (read-binary-file file) java.nio.ByteOrder/LITTLE_ENDIAN)
        filetype (str2/tail file 4)
		editor-version (read-n-string buf 6)
		format-version (read-n-string buf 3)
		_ (read-byte buf)
		width (if (= filetype ".aws")
                (read-byte buf)
                30)
		height (if (= filetype ".aws")
                (read-byte buf)
                20)
		tileset (parse-tileset
                 (condp = filetype
                   ".awd"  (dec (int (read-byte buf)))
                   ".aws"  (int (read-byte buf))
                   0))
		terrain-data (parse-terrain-data
                      (doall (for [_ (range (* width height))]
                               (read-dword buf))))
		unit-data (parse-unit-data
                   (doall (for [_ (range (* width height))]
                            (read-dword buf)))
                   width height)
        name (read-when-possible buf
               (read-n-string buf (read-int32 buf)))
        author (read-when-possible buf
                 (read-n-string buf (read-int32 buf)))
        desc (read-when-possible buf
               (str2/replace (read-n-string buf (read-int32 buf)) 
                             #"\r\n"
                             "\n"))]
	(struct-map map-file 
      :filename file
	  :width width
	  :height height
	  :tileset tileset
	  :terrain-data terrain-data
	  :unit-data unit-data
      :name name
      :author author
      :description desc
      :type editor-version)))
