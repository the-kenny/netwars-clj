(ns netwars.core
  (:require [org.httpkit.server :as http]
            [netwars.net.rest :as rest]
            [clojure.tools.nrepl.server :as nrepl]
            [netwars.net.page.game :as game-page]
            [compojure.route :as route]
            [ring.util.response :as response])
  (:use compojure.core
        netwars.logging))

(def webapp-url  "http://localhost")
(def webapp-port 8080)
(def nrepl-port  4006)

(defroutes main-routes
  (GET "/" [] (response/redirect "/game"))
  (GET "/game" [] (#'game-page/page))
  (route/resources "/")
  ;; Api
  (context "/api" [] #'rest/api)
  (route/not-found "<p>aww... this doesn't exist</p>"))

(defonce nrepl-server (do (nrepl/start-server :port nrepl-port)
                          (println (str "Started nrepl-server on port " nrepl-port))))
(defonce server (atom nil))

(defn start []
  (reset! server (http/run-server
                  #'main-routes
                  {:port webapp-port :websocket true}))
  (log "server started"))

(defn stop []
  (when @server
    (@server)
    (reset! server nil)))

(defn -main []
  (start))
