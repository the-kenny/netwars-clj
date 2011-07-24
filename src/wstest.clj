(use 'lamina.core
     'aleph.http)

(def broadcast-channel (permanent-channel))

(defn test-handler [ch handshake]
  (siphon broadcast-channel ch))

(def server (start-http-server #'test-handler {:port 8080 :websocket true}))

;;; Netwars stuff
(use '[clojure.contrib.base64 :as base64]
     '[netwars.map-loader :as map-loader]
     '[netwars.map-drawer :as map-drawer]
     '[netwars.unit-loader :as unit-loader]
     '[netwars.game-board :as board]
     '[clojure.java.io :as io]
     '[clojure.contrib.json :as json])

(import '[org.apache.commons.codec.binary Base64])

(defn serve-terrain-image [loaded-map]
  (let [img (map-drawer/render-terrain-board (:terrain loaded-map))
        os (java.io.ByteArrayOutputStream.)
        output (java.io.StringWriter.)]
    (javax.imageio.ImageIO/write img "png" os)
    (str "data:image/png;base64,"
         (Base64/encodeBase64String (.toByteArray os)))))

(let [loaded-map (map-loader/load-map "maps/7330.aws")
      unit-spec  (unit-loader/load-units "resources/units.xml")
      board      (board/generate-game-board loaded-map unit-spec)
      units (for [[{:keys [x y]} u] (:units board)] [(str x "," y) u])]
 (enqueue broadcast-channel
          (json/json-str {:map_image (serve-terrain-image loaded-map)
                          :units units})))
