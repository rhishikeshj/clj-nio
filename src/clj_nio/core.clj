(ns clj-nio.core
  (:import
     [java.net InetSocketAddress]
     [java.nio ByteBuffer CharBuffer]
     [java.nio.channels ServerSocketChannel SocketChannel Selector SelectionKey]
     [java.nio.charset Charset CharsetEncoder CharsetDecoder])
  (:require [clojure.tools.logging :as ctl]
            [clojure.stacktrace :as clj-stk]
            [clojure.data.json :as json]))

(defonce server-running? true)


(defn- selector [server-socket-channel]
  (let [selector (Selector/open)]
    (.register server-socket-channel selector SelectionKey/OP_ACCEPT)
    selector))


(defn- setup
  [server port]
  (let [server-socket-channel (ServerSocketChannel/open)
        _ (.configureBlocking server-socket-channel false)
        server-socket (.socket server-socket-channel)
        inet-socket-address (InetSocketAddress. ^String server ^Integer port)]
    (.bind server-socket inet-socket-address)
    [(selector server-socket-channel)
     server-socket]))


(defn state= [state channel]
  (= (bit-and (.readyOps channel) state) state))


(defn- buffer->string
  ([byte-buffer]
   (buffer->string byte-buffer (Charset/defaultCharset)))
  ([byte-buffer ^sun.nio.cs.UTF_8 charset]
     (.. charset newDecoder (decode byte-buffer) toString)))


(defn- string->buffer
  ([string]
   (string->buffer string (Charset/defaultCharset)))
  ([string ^sun.nio.cs.UTF_8 charset]
     (.. charset newEncoder (encode (CharBuffer/wrap ^String string)))))


(defn- accept-connection [server-socket selector]
  (doto (.. server-socket accept getChannel)
    (.configureBlocking false)
    (.register selector SelectionKey/OP_READ)))


(defn- read-socket [selected-key read-callback]
  (let [socket-channel (.channel selected-key)
        buffer (ByteBuffer/allocate 4194304)]
    (.clear buffer)
    (.read socket-channel buffer)
    (.flip buffer)
    (if (= (.limit buffer) 0)
      (do
        (.cancel selected-key)
        (.close (.socket socket-channel)))
      (read-callback (json/read-str (buffer->string buffer) :key-fn keyword)))))



(defn- run-server [selector server-socket read-callback]
  (try
    (while server-running?
      (when (pos? (.select selector))
        (let [selected-keys (.selectedKeys selector)]
          (doseq [k selected-keys]
            (condp state= k
              SelectionKey/OP_ACCEPT
              (accept-connection server-socket selector)
              SelectionKey/OP_READ
              (read-socket k read-callback)))
          (.clear selected-keys))))
    (catch Throwable t
      (ctl/error "NIO server error : " (clj-stk/print-stack-trace t)))))


(defn start-nio-server
  ([read-callback]
     (start-nio-server read-callback "127.0.0.1" 9006))
  ([read-callback server]
     (start-nio-server read-callback server 9006))
  ([read-callback server port]
     (alter-var-root #'server-running? (constantly true))
     (future (apply run-server (conj (setup server port) read-callback)))))


(defn stop-nio-server
  []
  (alter-var-root #'server-running? (constantly false)))


(defn setup-nio-client-socket
  [server port]
  (let [client (SocketChannel/open)
        inet-socket-address (InetSocketAddress. ^String server ^Integer port)]
    (.connect client inet-socket-address)
    (ctl/info "Client connection is " client)
    client))


(defn write-to-nio-client-socket
  [client data]
  (let [^ByteBuffer buffer (string->buffer (json/write-str data))]
    (while (.hasRemaining buffer)
      (.write client buffer))))


(comment (cnc/start-nio-server (fn
                                 [data]
                                 (println data))
                               "127.0.0.1"
                               9006))


(comment (def *c
           (cnc/setup-nio-client-socket "127.0.0.1" 9006)))


(comment (cnc/write-to-nio-client-socket *c {:a "Rhi"}))
