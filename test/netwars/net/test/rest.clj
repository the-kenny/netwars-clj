(ns netwars.net.test.rest
  (:use midje.sweet
        netwars.net.rest
        [netwars.core :as core]
        [netwars.net.game-server :as game-server]
        [clojure.data.json :as json]))

(background (before :contents (do
                                (core/start)
                                (game-server/start-new-game {:map-name "7330.aws"})))
            (after  :contents (core/stop)))

(fact
  (json/read-json (main)) => {:api-version 1.0})

(fact
  (json/read-json (games)) =>  (contains {:count integer? :ids sequential?}))

(fact
  (let [id (first (:ids (json/read-json (games))))]
    id => string?
    (json/read-json (game id)) => (contains {:info map?
                                             :players sequential?
                                             :moves sequential?
                                             :current-player-index integer?
                                             :round-counter integer?})))
