(ns netwars.net.map-server
  (:require [netwars.net.connection :as connection]
            [netwars.map-loader :as map-loader]
            [netwars.map-drawer :as map-drawer]
            [netwars.net.otw :as otw])
  (:use [clojure.contrib.def :only [defn-memo]])
  (:import [org.apache.commons.codec.binary Base64]))


;;; TODO: Serve via http
(defn send-map-data [client board]
  (let [map-image (map-drawer/render-terrain-board (:terrain board))]
   (connection/send-data client {:type :request-map
                                 :map-data map-image})))

(defmethod connection/handle-request :request-map [client request]
  (println "Got map request:" (str request))
  (let [requested-map (:map request)]
    (send-map-data client requested-map)))
