(ns netwars.core
  (:use compojure.core
        clojure.tools.logging
        clj-logging-config.log4j
        [aleph.http :only [start-http-server
                           wrap-aleph-handler
                           wrap-ring-handler]]
        [clojure.java.browse :only [browse-url]]
        [ring.util.response :only [redirect]])
  (:require [netwars.net.rest :as rest]

            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            
            [netwars.net.page.game :as game-page]

            [compojure.route :as route]
            [ring.middleware.stacktrace :as ringtrace]))

(def webapp-url "http://localhost")
(def webapp-port 8080)

(defroutes main-routes
  (GET "/" [] (redirect "/game"))
  (GET "/game" [] (#'game-page/page))
  (route/resources "/")
  ;; Api
  (context "/api" [] #'rest/api)
  (route/not-found "<p>aww... this doesn't exist</p>"))

(defonce nrepl-server (try (start-server :port 4006)
                           (catch java.lang.Exception e
                             (error e "Failed to start nrepl-server"))))
(defonce server (atom nil))

(defn start [& open-browser]
  (set-loggers!
   "netwars.net"
   {:level :info
    :pattern "%d %p: %m%n"
    :out #_(java.io.File. "netwars.log") :console})
  (reset! server (start-http-server (-> #'main-routes
                                        ringtrace/wrap-stacktrace
                                        wrap-ring-handler)
                                    {:port webapp-port :websocket true}))
  (info "server started")
  (when open-browser
    (browse-url (str webapp-url ":" webapp-port))))

(defn stop []
  (when @server
    (@server)
    (reset! server nil)
    (info "Server stopped")))

(defn -main []
  (set-loggers!
   "netwars"
   {:level :info
    :pattern "%d %p: %m%n"
    :out (java.io.File. "netwars.log")})
  (start true))
