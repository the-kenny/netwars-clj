(ns netwars.core
  (:use [netwars.net.connection :as connection]
        [netwars.net.game-server :as game-server]
        [aleph.http :only [start-http-server
                           wrap-aleph-handler
                           wrap-ring-handler]]
        compojure.core
        [compojure.route :as route]
        [compojure.handler :as handler]
        [ring.util.response :only [redirect]]
        clojure.tools.logging
        clj-logging-config.log4j))

#_(set-loggers!
 "netwars.net"
 {:level :info
  :pattern "%d %p: %m%n"
  :out #_(java.io.File. "netwars.log") :console})

(defroutes main-routes
  (GET "/" [] (redirect "/netwars.html"))
  (route/resources "/")
  (GET "/socket" [] (wrap-aleph-handler connection/websocket-handler))
  (route/not-found "<p>aww... this doesn't exist</p>"))

(let [server (atom nil)]
  (defn start []
   (reset! server (start-http-server (wrap-ring-handler #'main-routes)
                                     {:port 8080 :websocket true}))
   (info "server started"))

  (defn stop []
    (@server)
    (reset! server nil)
    (info "Server stopped")))

(defn -main []
  (set-loggers!
   "netwars"
   {:level :info
    :pattern "%d %p: %m%n"
    :out (java.io.File. "netwars.log")})
  (start))
