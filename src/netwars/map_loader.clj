(ns netwars.map-loader
  (:use [clojure.contrib.def :only [defalias]]
        netwars.unit-loader)
  (:import java.nio.ByteBuffer))

(defstruct map-file
  :filename
  :name
  :author
  :description
  :type
  :tileset
  :terrain-data
  :unit-data)


(def #^{:private true} tilesets
	 {0 :normal
	  1 :snow
	  2 :desert
	  3 :wasteland
	  4 :aw1
	  5 :aw2})


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
	 {0 :headquarter
	  1 :city
	  2 :base
	  3 :airport
	  4 :port
	  5 :tower
	  6 :lab})

(def #^{:private true} terrain-color-values
	{0 :red
	 1 :blue
	 2 :green
	 3 :yellow
	 4 :black
	 5 :white})

(defalias unit-color-values terrain-color-values)

(defn read-binary-file [#^String uri]
  (let [file (java.io.File. uri)
		buf (java.nio.ByteBuffer/allocate (.length file))]
	(.. (java.io.FileInputStream. file) (getChannel) (read buf))
	(.rewind buf)))

(defn read-n-string [buf len]
  (let [arr (make-array Byte/TYPE len)]
    (.get buf arr)
    (apply str (map char  arr))))

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
   (if-let [terr (get terrain-building-values (rem (- value 300) 10))]
     [terr (get terrain-color-values (rem (- value 300) 10))])
   ;; (<= 900 value 1299) ni
   ;; (+ 900 (rem (- value 900) 20)
   ;;    (* 20 (int (/ (- value 900) 20))))
   ))

(defn parse-terrain-data [data]
  (map #(if-let [ret (parse-terrain %)]
          ret
          :unimplemented)
       data))

(defn parse-unit-data [data]
  ;; Not yet functional
  ;; (map (fn [value]
  ;;   	 ;; (println (short value))
  ;;   	 (when (< 499 value 900)
  ;;   	   (when-let [color (get unit-color-values
  ;;                                (int (java.lang.Math/floor
  ;;                                     (/ (- value 500) 40))))]
  ;;   		 {:value (int (/ (- value 500) 40))
  ;;   		  :color color}))) data)
  nil)
		 

(def aws-spec '[[:editor-version 6 :string]
				[:format-version 3 :string]
				[nil 1 :byte]
				[:width 1 :byte]
				[:height 1 :byte]
				[:tileset 1 :byte parse-tileset]
				[:terrain-data (* :width :height 2) :dword parse-terrain-data]
				[:unit-data (* :width :height 2) :dword parse-unit-data]])

(defn parse-map [file] 
  (let [ ;; I hope Little-Endianess doesn't cause problems
        buf (.order (read-binary-file file) java.nio.ByteOrder/LITTLE_ENDIAN)
		editor-version (read-n-string buf 6)
		format-version (read-n-string buf 3)
		_ (read-byte buf)
		width (read-byte buf)
		height (read-byte buf)
		tileset (parse-tileset (int (read-byte buf)))
		terrain-data (parse-terrain-data
                      (doall (for [_ (range (* width height))]
                               (read-dword buf))))
		unit-data (parse-unit-data
                   (doall (for [_ (range (* width height))]
                            (read-dword buf))))]
	(struct-map map-file
      :filename file
	  :width width
	  :height height
	  :tileset tileset
	  :terrain-data terrain-data
	  :unit-data unit-data)
    ))
