(ns netwars.net.connection
  (:use aleph.http
        [clojure.tools.logging :only [debug info warn error fatal]]
        netwars.net.otw)
  (:require [lamina.core :as lamina]))

(defrecord ClientConnection [client-id connection])

(defn- make-client-connection [id ch]
  (ClientConnection. id ch))

(defrecord BroadcastChannel [clients]
  Object
  (toString [self] (str "#<Broadcast " @clients ">")))

(defmethod print-method BroadcastChannel [b w]
  (print-simple (str b) w))

(defn make-broadcast-channel []
  (BroadcastChannel. (atom #{})))

(defn broadcast-clients [b]
  @(:clients b))

(defn add-broadcast-receiver! [broadcast client]
  (info "adding client" (:client-id client) "to broadcast" broadcast)
  (swap! (:clients broadcast) conj client))

(defn remove-broadcast-receiver! [broadcast client]
  (info "removing client" (:client-id client) "from broadcast" broadcast)
  (swap! (:clients broadcast) disj client))

(defn send-data [client data]
  (debug "Sending data" data "to" (:client-id client))
  (if-not (lamina/closed? (:connection client))
   (lamina/enqueue (:connection client) (encode-data data))
   (error "Attempted to send to closed channel of client:" (:client-id client))))

(defn send-broadcast [broadcast data]
  (debug "sending broadcast to" (count (broadcast-clients broadcast)) "clients")
  (doseq [c @(:clients broadcast)]
    (send-data c data)))


(def connection-pool (atom {}))

(def broadcast-channel (make-broadcast-channel))
(def connect-channel (lamina/permanent-channel))
(def disconnect-channel (lamina/permanent-channel))

(defmulti handle-request (fn [client data] (get data :type)))

(defn- enqueue-disconnect [client]
  (info "Got disconnect:" (:client-id client))
  (lamina/enqueue disconnect-channel client))

(defn- enqueue-connect [ch handshake]
  (let [c (make-client-connection (java.util.UUID/randomUUID) ch)]
    (info "Got new connection:" (:client-id c))
    (lamina/enqueue connect-channel c)
    (lamina/on-closed ch #(enqueue-disconnect c))
    (lamina/receive-all ch #(when (string? %)
                       (let [data (decode-data %)]
                         (debug "Got data:" data)
                        (handle-request c data))))
    (add-broadcast-receiver! broadcast-channel c)))


(defn on-disconnect [f]
  (lamina/receive-all disconnect-channel f))

(defn on-connect [f]
  (lamina/receive-all connect-channel f))

(on-connect #(swap! connection-pool assoc (:client-id %) %))
(on-disconnect (fn [client]
                 (remove-broadcast-receiver! broadcast-channel client)
                 (swap! connection-pool dissoc (:client-id client))))


(defmacro defresponse [type [client-sym request-sym] & body]
  `(defmethod netwars.net.connection/handle-request ~type [client# request#]
     (let [~request-sym request#
           ~client-sym client#]
       (try
         (send-data client# (merge request# (do ~@body)))
         (catch java.lang.Exception e#
           (error e#))
         (catch java.lang.Error e#
           (error e#))))))

(defresponse :ping [client request]
  (debug "Got ping from:" (:client-id client))
  {:type :pong})

(defmethod handle-request :default [client request]
  (error "Got unknown request:" request))

(def websocket-handler #'enqueue-connect)
