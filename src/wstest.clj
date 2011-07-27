(use 'lamina.core
     'aleph.http)

(def broadcast-channel (permanent-channel))

(defn send-data [ch data]
  (enqueue ch (pr-str (into {} (for [[k v] data] [(name k) v])))))

(def foo (atom []))
(defn register-game-handlers [client-channel]
  ;; (receive-all client-channel #(if (= )))
  (siphon broadcast-channel client-channel)
  (receive-all client-channel
               #(let [v (read-string %)]
                  (println v)
                  (when (= (get v "type") "request-map")
                    (send-data
                     client-channel
                     {"id" (get v "id")
                      "map-data"
                      (serve-terrain-image (map-loader/load-map "maps/7330.aws"))}))))
  )

(defn client-handler [ch handshake]
  (send-data ch {:id "welcome-message"
                 :welcome "Welcome to Netwars!"})
  (#'register-game-handlers ch))

(defonce server
  (start-http-server #'client-handler {:port 8080 :websocket true}))

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
   (seq units)
   ;; (reduce #(assoc-in %1 [(:x (key %2)) (:y (key %2))] (val %2)) {} units)
   ))

(let [loaded-map (map-loader/load-map "maps/7330.aws")
      unit-spec  (unit-loader/load-units "resources/units.xml")
      board      (board/generate-game-board loaded-map unit-spec)]
  (enqueue broadcast-channel
           (json/json-str {:type :initialize-map
                           :map_image (serve-terrain-image loaded-map)
                           :units (unit-to-json (:units board))
                           :message "Starting game..."})))
