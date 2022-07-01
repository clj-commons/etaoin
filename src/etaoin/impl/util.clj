(ns ^:no-doc etaoin.impl.util
  (:import
   [java.io File IOException]
   [java.net InetSocketAddress ServerSocket Socket]))

(set! *warn-on-reflection* true)

(defn map-or-nil?
  [x]
  (or (map? x) (nil? x)))

(defn deep-merge
  [& vals]
  (if (every? map-or-nil? vals)
    (apply merge-with deep-merge vals)
    (if (every? sequential? vals)
      (apply concat vals)
      (last vals))))

(defmacro defmethods
  "Declares multimethods in batch. For each dispatch value from
  dispatch-vals, creates a new method."
  [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))

(defn sec->ms [sec]
  (* sec 1000))

(defn ms->sec [ms]
  (/ ms 1000))

(defn dispatch-types
  [& args]
  (mapv class args))

(defn error
  ([msg]
   (throw (Exception. ^String msg)))
  ([tpl & args]
   (error (apply format tpl args))))

(defn get-free-port []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn connectable?
  "Returns `true` when it is possible to connect a given `host` over `port`."
  [host port]
  (with-open [socket (Socket.)]
    (try
      (.connect socket (InetSocketAddress. ^String host ^int port) 1000)
      true
      (catch IOException _e
        false))))

(defn exit
  [code template & args]
  (let [out (if (zero? code)
              *out*
              *err*)]
    (binding [*out* out]
      (println (apply format
                      template args))))
  (System/exit code))

(defmacro with-tmp-file [prefix suffix bind & body]
  `(let [tmp#  (File/createTempFile ~prefix ~suffix)
         ~bind (.getAbsolutePath tmp#)]
     (try
       ~@body
       (finally
         (.delete tmp#)))))

(defn strip-url-creds
  "Return `url` with any http credentials stripped, https://user:pass@hello.com -> https://hello.com.
  Use when logging urls to avoid spilling secrets."
  ^String [^String url]
  (let [u (java.net.URL. url)]
    (.toExternalForm
      (java.net.URL.
        (.getProtocol u)
        (.getHost u)
        (.getPort u)
        (.getFile u)
        (.getRef u)))))

(defn assoc-some
  "Associates a key with a value in a map, if and only if the value is
  not nil. From medley."
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))
