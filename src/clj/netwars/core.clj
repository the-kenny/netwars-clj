(ns netwars.core
  (:use [netwars.net.connection :as connection]
        [netwars.net.game-server :as game-server]
        [netwars.net.rest :as rest]
        [aleph.http :only [start-http-server
                           wrap-aleph-handler
                           wrap-ring-handler]]
        compojure.core
        [compojure.route :as route]
        [compojure.handler :as handler]
        [ring.util.response :only [redirect]]
        [ring.middleware.stacktrace :as ringtrace]
        clojure.tools.logging
        clj-logging-config.log4j))

(set-loggers!
 "netwars.net"
 {:level :info
  :pattern "%d %p: %m%n"
  :out #_(java.io.File. "netwars.log") :console})


(defroutes main-routes
  (GET "/" [] (redirect "/netwars.html"))
  (route/resources "/")
  (GET "/socket" [] (wrap-aleph-handler connection/websocket-handler))
  ;; Api
  (GET "/api" [] (rest/main))
  (GET "/api/games" [] (rest/games))
  (GET "/api/game/:id" [id] (rest/game id))
  (route/not-found "<p>aww... this doesn't exist</p>"))

(let [server (atom nil)]
  (defn start []
   (reset! server (start-http-server (-> #'main-routes
                                         ringtrace/wrap-stacktrace
                                         wrap-ring-handler)
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
