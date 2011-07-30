(ns netwars.connection
  (:require [goog.events :as events]
            [goog.json :as json]
            [goog.Timer :as timer]
            [cljs.reader :as reader]))

(defn log [& args]
  (.log js/console (apply str args)))

(defn encode-data [data]
  (pr-str (into {} (for [[k v] data] [(name k) v]))))

(defn decode-data [s]
  (into {} (for [[k v] (cljs.reader/read-string s)] [(keyword k) v])))

(defn- generate-id [& [prefix]]
  (apply str prefix (repeatedly 10 #(rand-int 10))))



(defmulti handle-response #(get % "type"))

(defmethod handle-response :default [message]
  (log "Got unknown message with type: " (get message "type")))

(defn send-data [socket data]
  (let [id (generate-id "send-data")]
    (.send socket (encode-data data))))

;;; Connnection Stuff
(defn handle-socket-message [socket-event]
  (log "got message: " (.data socket-event))
  (let [obj (reader/read-string (.data socket-event))]
   (handle-response obj)))

(let [closefns (atom [])]
  (defn on-close [f]
    (swap! closefns conj f))

  (defn handle-close [socket]
    (log "socket closed")
    (doseq [f @closefns]
      (when (fn? f) (f)))))

(let [openfns (atom [])]
  (defn on-open [f]
    (swap! openfns conj f))

  (defn handle-open [socket]
    (log "socket opened")
    (doseq [f @openfns]
      (when (fn? f) (f socket)))))

(defn start-ping-timer [interval ws]
  (let [t (goog.Timer. interval)]
    (events/listen t goog.Timer/TICK
                   (fn []
                     (send-data ws {"type" "ping"})))
    (. t (start))))

(defmethod handle-response "pong" [_]
  ;; TODO: Implement a timeout for reconnecting here
)

(on-open (partial start-ping-timer 5000))

(defn open-socket [uri]
  (let [ws (js/WebSocket. uri)]
    (events/listen ws "open" #(handle-open ws))
    (events/listen ws "close" #(handle-close ws))
    (set! (. ws onmessage) handle-socket-message)
    ws))
