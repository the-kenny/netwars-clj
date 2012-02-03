(ns netwars.net.test.rest
  (:use midje.sweet
        netwars.net.rest
        [netwars.core :as core]
        [netwars.net.game-server :as game-server]
        [clojure.data.json :as json]))

(defn- start-dummy-game []
  (dosync
   (game-server/store-game! (-> (game-server/start-new-game {:map-name "7330.aws"})
                                (assoc :game-id (java.util.UUID/randomUUID))))))

;;; NOTE: There seems to be a bug. This doesn't work if you use `background' instead of
;;;       `against-background'.
(against-background [(before :contents (do
                                         (core/start)
                                         (start-dummy-game)))
                     (after :contents (core/stop))]

  (fact
    (json/read-json (main)) => {:api-version 1.0})

  (fact
    (json/read-json (games)) => (contains {:count integer? :ids sequential?})
    (count (:ids (json/read-json (games)))) => 1 ;make sure we have a game running
    )

  (fact
    (let [id (first (:ids (json/read-json (games))))]
      id => string?
      (json/read-json (game id)) => (contains {:info map?
                                               :players sequential?
                                               :moves sequential?
                                               :current-player-index integer?
                                               :round-counter integer?}))))
