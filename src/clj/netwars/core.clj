(ns netwars.core
  (:use [netwars.net.connection :as connection]
        [netwars.net.game-server :as game-server]
        [netwars.net.rest :as rest]

        [netwars.net.page.game :as game-page]

        [aleph.http :only [start-http-server
                           wrap-aleph-handler
                           wrap-ring-handler]]
        compojure.core
        [compojure.route :as route]
        [ring.util.response :only [redirect]]
        [ring.middleware.stacktrace :as ringtrace]
        [clojure.java.browse :only [browse-url]]
        clojure.tools.logging
        clj-logging-config.log4j))

(def webapp-url "http://localhost")
(def webapp-port 8080)

(defroutes main-routes
  (GET "/" [] (redirect "/netwars.html"))
  (GET "/netwars.html" [] (game-page/page))
  (route/resources "/")
  (GET "/socket" [] (wrap-aleph-handler connection/websocket-handler))
  ;; Api
  (context "/api" [] rest/api-routes)
  (route/not-found "<p>aww... this doesn't exist</p>"))

(def ^:dynamic *server* (atom nil))


(defn start [& open-browser]
  (set-loggers!
   "netwars.net"
   {:level :info
    :pattern "%d %p: %m%n"
    :out #_(java.io.File. "netwars.log") :console})
  (reset! *server* (start-http-server (-> #'main-routes
                                        ringtrace/wrap-stacktrace
                                        wrap-ring-handler)
                                    {:port webapp-port :websocket true}))
  (info "server started")
  (when open-browser
    (browse-url (str webapp-url ":" webapp-port))))

(defn stop []
  (when @*server*
    (@*server*)
    (reset! *server* nil)
    (info "Server stopped")))

(defn -main []
  (set-loggers!
   "netwars"
   {:level :info
    :pattern "%d %p: %m%n"
    :out (java.io.File. "netwars.log")})
  (start true))
