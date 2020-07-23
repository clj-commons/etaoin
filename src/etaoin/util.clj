(ns etaoin.util)

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
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn connectable?
  "Checks whether it's possible to connect a given host/port pair."
  [host port]
  (when-let [^java.net.Socket socket
             (try
               (java.net.Socket. ^String host ^int port)
               (catch java.io.IOException _))]
    (when (.isConnected socket)
      (.close socket)
      true)))
