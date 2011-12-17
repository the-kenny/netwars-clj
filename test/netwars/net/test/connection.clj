(ns netwars.net.test.connection
  (:use clojure.test
        [netwars.net.connection :as connection]
        lamina.core
        clj-logging-config.log4j)
  (:import [netwars.net.connection ClientConnection]))

(set-loggers!
 "netwars.net.connection"
 {:level :warn
  :pattern "%d %p: %m%n"})

(deftest test-broadcast-channel
  (let [broadcast (connection/make-broadcast-channel)
        verify-atom (atom 0)            ;Every client will inc this when it gets data
        client-factory #(let [cc (ClientConnection. (java.util.UUID/randomUUID)
                                                    (channel))]
                            (receive-all (:connection cc)
                                         (fn [_] (swap! verify-atom inc)))
                            cc)
        c1 (client-factory)
        c2 (client-factory)]
    (is (= 0 (count (connection/broadcast-clients broadcast))))
    (add-broadcast-receiver! broadcast c1)
    (is (= 1 (count (connection/broadcast-clients broadcast))))
    (add-broadcast-receiver! broadcast c2)
    (is (= 2 (count (connection/broadcast-clients broadcast)))) ;6 receiver
    (send-broadcast broadcast {})
    (is (= 2 @verify-atom))             ;Check if all receivers got the message
    (reset! verify-atom 0)
    (connection/remove-broadcast-receiver! broadcast c1) ;Remove receiver 1
    (send-broadcast broadcast {})
    (is (= 1 @verify-atom))))
