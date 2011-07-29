(ns netwars.connection
  (:require [goog.events :as events]
            [goog.json :as json]
            [goog.Timer :as timer]
            [cljs.reader :as reader]))

(defn log [& args]
  (.log js/console (apply str args)))

;;; Hacky implementation of multimethod-dispatch
(let [dispatch-spec (atom {})]
  (defn get-callback-fn [key]
    (get @dispatch-spec key))
  (defn register-callback! [key fn]
    (swap! dispatch-spec assoc key fn))
  (defn unregister-callback! [key]
    (swap! dispatch-spec dissoc key)))

(defn call-callback
  "Calls callback for key with args"
  [key data-object]
  (if-let [f (get-callback-fn key)]
    (f data-object)
    (log "No callback specified for type " key)))

(defn- generate-id [& [prefix]]
  (apply str prefix (repeatedly 10 #(rand-int 10))))

(defn send-data
  ([socket data callback]
     (let [id (generate-id "send-data")]
       (register-callback! id #(do (callback %)
                                   (unregister-callback! id)))
       (let [js (pr-str (assoc data "id" id))]
         (.send socket js))))
  ([socket data]
     (let [p (atom)]
       (send-data socket data #(reset! p %))
       p)))

;;; Connnection Stuff
(defn handle-message [socket-event]
  (log "got data: " (.data socket-event))
  (let [obj #_(json/parse (.data socket-event))
        (reader/read-string (.data socket-event))
        id (get obj "id")]
   (call-callback id obj)))

(defn handle-close [socket]
  (log "socket closed"))

(defn handle-open [socket]
  (log "socket opened")
  ;; Schedule periodic pings
  (let [t (goog.Timer. 2000)]
    (events/listen t goog.Timer/TICK
                   (fn [data]
                     (log "Ping...")
                     (send-data socket {"type" "ping"}
                                #(log "Pong"))))
    (. t (start))))

(defn open-socket [uri]
  (let [ws (js/WebSocket. uri)]
    (events/listen ws "open" #(handle-open ws))
    (events/listen ws "close" #(handle-close ws))
    (set! (. ws onmessage) handle-message)
    ws))
