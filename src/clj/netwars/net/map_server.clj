(ns netwars.net.map-server
  (:require [netwars.net.connection :as connection]
            [netwars.map-loader :as map-loader]
            [netwars.map-drawer :as map-drawer])
  (:use [clojure.contrib.def :only [defn-memo]])
  (:import [org.apache.commons.codec.binary Base64]))

(def base-map-path "maps/")

(defn safe-load-map [file]
  (let [file (-> file
                 (.replace java.io.File/separatorChar \?)
                 (.replace ".." "??"))]
    (map-loader/load-map (str base-map-path file))))

(defn-memo map-to-base64 [file]
  (let [loaded-map (safe-load-map file)
        img (map-drawer/render-terrain-board (:terrain loaded-map))]
    (connection/image-to-base64 img)))

(defn send-map-data [client map-file]
  (let [map-base64 (map-to-base64 map-file)]
   (connection/send-data client {:type :request-map
                                 :map-data map-base64})))

(defmethod connection/handle-request :request-map [client request]
  (println "Got map request:" (str request))
  (let [requested-map (:map request)]
    (send-map-data client requested-map)))
