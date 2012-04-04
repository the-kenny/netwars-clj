(ns netwars.map-loader
  (:use netwars.aw-map
        netwars.binary-data))

(defrecord AwMapUnit [id color])
(defrecord LoadedMap [^netwars.aw_map.TerrainBoard terrain units info])

(let [tilesets [:normal :snow :desert :wasteland :aw1 :aw2]]
  (defn parse-tileset [byte]
   (get tilesets byte)))

(let [terrain-values  {0   :plain
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
                       39  :beach}
      terrain-building-values [:headquarter :city :base :airport :port :tower :lab]
      terrain-color-values [:red :blue :green :yellow :black :white]
      unit-color-values terrain-color-values]

  (defn parse-terrain [value]
    (cond
     (<= 0 value 299)
     (get terrain-values (+ (rem value 30)
                            (* 30 (int (/ value 30)))))
     (<= 300 value 499)
     (when-let [terr (get terrain-building-values (rem (- value 300) 10))]
       (let [color (get terrain-color-values (int (/ (- value 300) 10)))]
         (if (and (= terr :headquarter) (= color :white))
           [:silo color]
           [terr color])))))

  (defn parse-unit [value]
    (AwMapUnit. (rem (- value 500) 40) (get unit-color-values
                                            (int (/ (- value 500) 40))))))


(defn parse-terrain-data [data]
  (map #(if-let [ret (parse-terrain %)]
          ret
          :unimplemented)
       data))

(defn parse-unit-data [data width height]
  (into {}
        (for [x (range width) y (range height)
              :let [val (nth data (+ (* x height) y))]
              :when (not= val -1)]
          [(coord x y) (parse-unit val)])))

(def aws-spec '[[:editor-version 6 :string]
				[:format-version 3 :string]
				[nil 1 :byte]
				[:width 1 :byte]
				[:height 1 :byte]
				[:tileset 1 :byte parse-tileset]
				[:terrain-data (* :width :height 2) :dword parse-terrain-data]
				[:unit-data (* :width :height 2) :dword parse-unit-data]])

(defn- extract-colors [unit-data terrain-data]
  (let [units (vals unit-data)
        buildings (filter vector? (reduce concat (:data terrain-data)))]
    (disj (set (concat (map :color units) (map second buildings))) :white)))

(defn load-map [source]
  (let [ ;; I hope Little-Endianess doesn't cause problems
        buf (.order (read-binary-resource source)
                    java.nio.ByteOrder/LITTLE_ENDIAN)
        filetype (last (.split source "\\."))
		editor-version (read-n-string buf 6)
		format-version (read-n-string buf 3)
		_ (read-byte buf)
		width (if (= filetype "aws")
                (read-byte buf)
                30)
		height (if (= filetype "aws")
                 (read-byte buf)
                 20)
		tileset (parse-tileset
                 (condp = filetype
                     "awd"  (dec (int (read-byte buf)))
                     "aws"  (int (read-byte buf))
                     0))
		terrain-data (vec (parse-terrain-data
                           (doall (for [_ (range (* width height))]
                                    (read-dword buf)))))
		unit-data (parse-unit-data
                   (doall (for [_ (range (* width height))]
                            (read-dword buf)))
                   width height)
        name (read-when-possible buf
                                 (read-n-string buf (read-int32 buf)))
        author (read-when-possible buf
                                   (read-n-string buf (read-int32 buf)))
        desc (read-when-possible buf
                                 (.replace (read-n-string buf (read-int32 buf))
                                           "\r\n"
                                           "\n"))
        terrain-board (make-terrain-board
                       [width height]
                       (vec (map vec (partition height terrain-data))))
        player-colors (extract-colors unit-data terrain-board)]

    (LoadedMap.
     terrain-board
     unit-data
     {:source source
      :tileset tileset
      :name name
      :author author
      :description desc
      :version editor-version
      :player-colors player-colors})))
