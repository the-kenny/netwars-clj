(ns netwars.core
  (:require [org.httpkit.server :as http]
            [netwars.net.rest :as rest]
            [clojure.tools.nrepl.server :as nrepl]
            [netwars.net.page.game :as game-page]
            [compojure.route :as route]
            [ring.util.response :as response])
  (:use compojure.core))

(def webapp-url "http://localhost")
(def webapp-port 8080)

(defroutes main-routes
  (GET "/" [] (response/redirect "/game"))
  (GET "/game" [] (#'game-page/page))
  (route/resources "/")
  ;; Api
  (context "/api" [] #'rest/api)
  (route/not-found "<p>aww... this doesn't exist</p>"))

(defonce nrepl-server (nrepl/start-server :port 4006))
(defonce server (atom nil))

(defn start [& open-browser]
  ;; (set-loggers!
  ;;  "netwars.net"
  ;;  {:level :info
  ;;   :pattern "%d %p: %m%n"
  ;;   :out #_(java.io.File. "netwars.log") :console})
  (reset! server (http/run-server
                  #'main-routes
                  {:port webapp-port :websocket true}))
  ;; (info "server started")
  ;; (when open-browser
  ;;   (browse-url (str webapp-url ":" webapp-port)))
  )

(defn stop []
  ;; (when @server
  ;;   (@server)
  ;;   (reset! server nil)
  ;;   (info "Server stopped"))
  )

(defn -main []
  ;; (set-loggers!
  ;;  "netwars"
  ;;  {:level :info
  ;;   :pattern "%d %p: %m%n"
  ;;   :out (java.io.File. "netwars.log")})
  (start true))
