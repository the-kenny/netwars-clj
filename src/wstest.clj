(use 'lamina.core
     'aleph.http)

(def broadcast-channel (permanent-channel))

(defn register-game-handlers [client-channel]
 ;; (receive-all client-channel #(if (= )))
  (siphon broadcast-channel client-channel))

(defn client-handler [ch handshake]
  (enqueue ch {:welcome  "Welcome to Netwars!"})
  (register-game-handlers ch))

(def server (start-http-server #'client-handler {:port 8080 :websocket true}))

;;; Netwars stuff
(use '[clojure.contrib.base64 :as base64]
     '[netwars.map-loader :as map-loader]
     '[netwars.map-drawer :as map-drawer]
     '[netwars.unit-loader :as unit-loader]
     '[netwars.game-board :as board]
     '[clojure.java.io :as io]
     '[clojure.contrib.json :as json]
     '[aleph.formats :as formats])

(import '[org.apache.commons.codec.binary Base64])

(defn serve-terrain-image [loaded-map]
  (let [img (map-drawer/render-terrain-board (:terrain loaded-map))
        os (java.io.ByteArrayOutputStream.)
        output (java.io.StringWriter.)]
    (javax.imageio.ImageIO/write img "png" os)
    (str "data:image/png;base64,"
         (Base64/encodeBase64String (.toByteArray os)))))

(defn unit-to-json [units]
  (formats/data->json->string
   (reduce #(assoc-in %1 [(:x (key %2)) (:y (key %2))] (val %2)) {} units)))

(let [loaded-map (map-loader/load-map "maps/7330.aws")
      unit-spec  (unit-loader/load-units "resources/units.xml")
      board      (board/generate-game-board loaded-map unit-spec)]
  (println (unit-to-json (:units board)))
 (enqueue broadcast-channel
          (json/json-str {:map_image (serve-terrain-image loaded-map)
                          :units (unit-to-json (:units board))})))
