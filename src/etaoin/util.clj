(ns etaoin.util
  (:import java.io.File
           java.nio.file.attribute.FileAttribute
           java.nio.file.Files
           org.apache.commons.io.FileUtils))

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

(defmacro with-tmp-dir [prefix bind & body]
  `(let [tmp#  (str (Files/createTempDirectory
                      ~prefix
                      (into-array FileAttribute [])))
         ~bind tmp#]
     (try
       ~@body
       (finally
         (FileUtils/deleteDirectory (File. tmp#))))))
