(ns netwars.connection
  (:require [goog.events :as events]
            [goog.json :as json]
            [goog.Timer :as timer]
            [cljs.reader :as reader]
            [netwars.logging :as logging]))

(def *socket* nil)

(defn encode-data [data]
  (pr-str (into {} (for [[k v] data] [(name k) v]))))

(defn decode-data [s]
  (logging/log s)
  (reader/read-string s))

(defn- generate-id [& [prefix]]
  (apply str prefix (repeatedly 10 #(rand-int 10))))

(defmulti handle-response :type)

(defmethod handle-response :default [message]
  (logging/log "Got unknown message with type: " (:type message)))

(defn send-data [data]
  (let [id (generate-id "send-data")]
    (logging/log "Sending data: " (encode-data data))
    (.send *socket* (encode-data data))))

;;; Connnection Stuff
(defn handle-socket-message [socket-event]
  (let [obj (decode-data (.data socket-event))]
    (handle-response obj)))

(let [closefns (atom [])]
  (defn on-close [f]
    (swap! closefns conj f))

  (defn handle-close []
    (logging/log "socket closed")
    (doseq [f @closefns]
      (when (fn? f) (f)))))

(let [openfns (atom [])]
  (defn on-open [f]
    (swap! openfns conj f))

  (defn handle-open []
    (logging/log "socket opened")
    (doseq [f @openfns]
      (when (fn? f) (f)))))

(defn start-ping-timer [interval]
  (let [t (goog.Timer. interval)]
    (events/listen t goog.Timer/TICK
                   #(send-data {:type :ping}))
    (. t (start))))

(defmethod handle-response :pong [_]
  ;; TODO: Implement a timeout for reconnecting here
)

#_(on-open #(start-ping-timer 5000))

(defn open-socket [uri]
  (let [ws (js/WebSocket. uri)]
    (events/listen ws "open" handle-open)
    (events/listen ws "close" handle-close)
    (set! (. ws onmessage) #(handle-socket-message %))
    (set! *socket* ws)))
