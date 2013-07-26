# Introduction to clj-nio

clj-nio
=======

NIO socket communication in clojure

Require the nio namespace
=======
(require '[clj-nio.core :as cnc])

Start nio server
=======
    (cnc/start-nio-server
              {:server "127.0.0.1"
              :port 9006
              :socket-read-fn (fn
                                [data]
                                (println data))})

Create nio client connection
=======
    (def *c
           (cnc/setup-nio-client-socket "127.0.0.1" 9006))

Write to nio client socket
=======
    (cnc/write-to-nio-client-socket *c {:nio-works true})
