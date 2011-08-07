(ns netwars.net.connection
  (:use lamina.core
        aleph.http
        [aleph.formats :as formats]
        [clojure.tools.logging :only [debug info warn error fatal]]
        netwars.net.otw))

(defrecord ClientConnection [client-id connection])

(defn- make-client-connection [id ch]
  (ClientConnection. id ch))

(defrecord BroadcastChannel [clients])

(defn make-broadcast-channel []
  (BroadcastChannel. (atom #{})))

(defn add-broadcast-receiver! [broadcast client]
  (debug "adding client" (:client-id client) "to broadcast" broadcast)
  (swap! (:clients broadcast) conj client))

(defn remove-broadcast-receiver! [broadcast client]
  (debug "removing client" (:client-id client) "from broadcast" broadcast)
  (swap! (:clients broadcast) disj client))

(defn send-data [client data]
  (debug "Sending data" data "to" (:client-id client))
  (if-not (closed? (:connection client))
   (enqueue (:connection client) (encode-data data))
   (error "Attempted to send to closed channel of client:" (:client-id client))))

(defn send-broadcast [broadcast data]
  (debug "sending broadcast to" (-> broadcast :clients count) "clients")
  (doseq [c @(:clients broadcast)]
    (send-data c data)))


(def connection-pool (atom {}))

(def broadcast-channel (make-broadcast-channel))
(def connect-channel (permanent-channel))
(def disconnect-channel (permanent-channel))

(defmulti handle-request (fn [client data] (get data :type)))


(defn- enqueue-disconnect [client]
  (info "Got disconnect:" (:client-id client))
  (enqueue disconnect-channel client))

(defn- enqueue-connect [ch handshake]
  (let [c (make-client-connection (java.util.UUID/randomUUID) ch)]
    (info "Got new connection:" (:client-id c))
    (enqueue connect-channel c)
    (on-closed ch #(enqueue-disconnect c))
    (receive-all ch #(when (string? %)
                       (handle-request c (decode-data %))))
    (add-broadcast-receiver! broadcast-channel c)))


(defn on-disconnect [f]
  (receive-all disconnect-channel f))

(defn on-connect [f]
  (receive-all connect-channel f))

(on-connect #(swap! connection-pool assoc (:client-id %) %))
(on-disconnect #(swap! connection-pool dissoc (:client-id %)))


(defmethod handle-request :ping [client request]
  (debug "Got ping from" (:client-id client))
  (send-data client (assoc request :type :pong)))

(defmethod handle-request :default [client request]
  (error "Got unknown message:" (str request))
  (send-data client request))

(defn start-server [port]
  (start-http-server #'enqueue-connect {:port port :websocket true}))

(defn stop-server [server]
  (server))
