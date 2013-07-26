# Introduction to clj-nio

clj-nio
=======

NIO socket communication in clojure

Require the nio namespace
=======
(require '[clj-nio.core :as cnc])

Start nio server
=======
    (cnc/start-nio-server (fn
                             [data]
                             (println data))
                          "127.0.0.1"
                          9006)

Create nio client connection
=======
    (def *c
           (cnc/setup-nio-client-socket "127.0.0.1" 9006))

Write to nio client socket
=======
    (cnc/write-to-nio-client-socket *c {:nio-works true})
