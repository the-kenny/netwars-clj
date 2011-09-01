(ns netwars.net.map-server
  (:use [clojure.tools.logging :only [debug info warn error fatal]])
  (:require [netwars.net.connection :as connection]
            [netwars.map-loader :as map-loader]
            [netwars.map-drawer :as map-drawer]
            [netwars.net.otw :as otw])
  (:import [org.apache.commons.codec.binary Base64]))


;;; TODO: Serve via http
(defn send-map-data [client board]
  (info "Sending map-data to client" client)
  (let [map-image (map-drawer/render-terrain-board (:terrain board))]
   (connection/send-data client {:type :request-map
                                 :map-data map-image})))

(defmethod connection/handle-request :request-map [client request]
  (info "Got map request:" (str request))
  (let [requested-map (:map request)]
    (send-map-data client requested-map)))
