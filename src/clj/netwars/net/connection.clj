(ns netwars.net.connection
  (:use lamina.core
        aleph.http
        [aleph.formats :as formats]
        netwars.net.otw))

(defrecord ClientConnection [client-id connection])

(defn- make-client-connection [id ch]
  (ClientConnection. id ch))

(defn make-broadcast-channel []
  (permanent-channel))

(defn add-broadcast-receiver [broadcast client]
  (siphon broadcast (:connection client)))


(defn send-broadcast [broadcast data]
  (if-not (closed? broadcast)
   (enqueue broadcast (encode-data data))
   (println "Attempted to send to closed broadcast-channel")))

(defn send-data [client data]
  (if-not (closed? (:connection client))
   (enqueue (:connection client) (encode-data data))
   (println "Attempted to send to closed channel")))


(def connection-pool (atom {}))

(def broadcast-channel (permanent-channel))
(def connect-channel (permanent-channel))
(def disconnect-channel (permanent-channel))

(defmulti handle-request (fn [client data] (get data :type)))


(defn- enqueue-disconnect [client]
  (enqueue disconnect-channel client))

(defn- enqueue-connect [ch handshake]
  (let [c (make-client-connection (java.util.UUID/randomUUID) ch)]
    (enqueue connect-channel c)
    (on-closed ch #(enqueue-disconnect c))
    (receive-all ch #(when (string? %)
                       (handle-request c (decode-data %))))
    (siphon broadcast-channel ch)))


(defn on-disconnect [f]
  (receive-all disconnect-channel f))

(defn on-connect [f]
  (receive-all connect-channel f))

(on-connect #(println "Got new connection with client-id:" (:client-id %)))
(on-connect #(swap! connection-pool assoc (:client-id %) %))

(on-disconnect #(println "Got disconnect from client:" (:client-id %)))
(on-disconnect #(swap! connection-pool dissoc (:client-id %)))


(defmethod handle-request :ping [client request]
  #_(println "Got ping from" (:client-id client))
  (send-data client (assoc request :type :pong)))

(defmethod handle-request :default [client request]
  (println "Got unknown message:" (str request))
  (send-data client request))

(defn start-server [port]
  (start-http-server #'enqueue-connect {:port port :websocket true}))

(defn stop-server [server]
  (server))
