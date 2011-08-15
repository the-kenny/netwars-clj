(ns netwars.connection
  (:require [goog.events :as events]
            [goog.json :as json]
            [goog.Timer :as timer]
            [cljs.reader :as reader]
            [netwars.logging :as logging]))

;;; TODO: Use only this socket
(def *socket* nil)

(defn encode-data [data]
  (pr-str (into {} (for [[k v] data] [(name k) v]))))

(defn decode-data [s]
  (logging/log s)
  (reader/read-string s))

(defn- generate-id [& [prefix]]
  (apply str prefix (repeatedly 10 #(rand-int 10))))

(defmulti handle-response (fn [server message] (:type message)))

(defmethod handle-response :default [server message]
  (logging/log "Got unknown message with type: " (:type message)))

(defn send-data
  ([server data]
     (let [id (generate-id "send-data")]
       (.send server (encode-data data))))
  ([data]
     (let [id (generate-id "send-data")]
       (.send *socket* (encode-data data)))))

;;; Connnection Stuff
(defn handle-socket-message [server socket-event]
  (let [obj (decode-data (.data socket-event))]
    (handle-response server obj)))

(let [closefns (atom [])]
  (defn on-close [f]
    (swap! closefns conj f))

  (defn handle-close [socket]
    (logging/log "socket closed")
    (doseq [f @closefns]
      (when (fn? f) (f)))))

(let [openfns (atom [])]
  (defn on-open [f]
    (swap! openfns conj f))

  (defn handle-open [socket]
    (logging/log "socket opened")
    (doseq [f @openfns]
      (when (fn? f) (f socket)))))

(defn start-ping-timer [interval ws]
  (let [t (goog.Timer. interval)]
    (events/listen t goog.Timer/TICK
                   (fn []
                     (send-data ws {:type :ping})))
    (. t (start))))

(defmethod handle-response :pong [_ _]
  ;; TODO: Implement a timeout for reconnecting here
)

(on-open (partial start-ping-timer 5000))

(defn open-socket [uri]
  (let [ws (js/WebSocket. uri)]
    (events/listen ws "open" #(handle-open ws))
    (events/listen ws "close" #(handle-close ws))
    (set! (. ws onmessage) (partial handle-socket-message ws))
    ws))
