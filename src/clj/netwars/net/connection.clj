(ns netwars.net.connection
  (:use lamina.core
        aleph.http
        [aleph.formats :as formats])
  (:import [org.apache.commons.codec.binary Base64]))

(defrecord ClientConnection [client-id connection])

(defn make-client-connection [id ch]
  (ClientConnection. id ch))

(defn encode-data [data]
  (binding [*print-meta* true]
   ;; (pr-str (into {} (for [[k v] data] [(name k) v])))
    (pr-str data)))

(defn decode-data [s]
  (into {} (for [[k v] (read-string s)] [(keyword k) v])))

(defn encode-coordinate [c]
  [(:x c) (:y c)])

(defn image-to-base64 [image]
  (let [os (java.io.ByteArrayOutputStream.)
        output (java.io.StringWriter.)]
    (javax.imageio.ImageIO/write image "png" os)
    (str "data:image/png;base64,"
         (Base64/encodeBase64String (.toByteArray os)))))

(defn send-data [client data]
  (if-not (closed? (:connection client))
   (enqueue (:connection client) (encode-data data))
   (println "Attempted to send to closed channel")))


(def connection-pool (atom {}))
(def broadcast-channel (permanent-channel))

(defmulti handle-request (fn [client data] (get data :type)))

(defn handle-disconnect [client]
  (println "Got disconnect from client:" (:client-id client))
  (swap! connection-pool dissoc (:client-id client)))

(defn handle-connect [ch handshake]
  (let [c (make-client-connection (java.util.UUID/randomUUID) ch)]
    (println "Got new connection with client-id:" (:client-id c))
    (swap! connection-pool assoc (:client-id c) c)
    (on-closed ch #(handle-disconnect c))
    (receive-all ch #(when (string? %)
                       (handle-request c (decode-data %))))
    (siphon broadcast-channel ch)))

(defmethod handle-request :ping [client request]
  #_(println "Got ping from" (:client-id client))
  (send-data client (assoc request :type :pong)))

(defmethod handle-request :default [client request]
  (println "Got unknown message:" (str request))
  (send-data client request))

(defn start-server [port]
  (start-http-server #'handle-connect {:port port :websocket true}))

(defn stop-server [server]
  (server))
