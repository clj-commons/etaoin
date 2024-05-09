(ns ^:no-doc etaoin.impl.proc
  (:require
   [babashka.fs :as fs]
   #?@(:bb [[babashka.process :as p]]
       :clj [[babashka.process :as p]
             [babashka.process.pprint]]) ;; to support exception rendering in REPL
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def windows? (str/starts-with? (System/getProperty "os.name") "Windows"))

(defn- get-null-file ^java.io.File
  []
  (if windows?
    (fs/file "NUL")
    (fs/file "/dev/null")))

(defn- redirect-opts [val std-key std-file-key]
  (cond-> {}
    (= :inherit val)
    (assoc std-key :inherit)

    (string? val)
    (assoc std-key :write
           std-file-key (fs/file val))

    (nil? val)
    (assoc std-key :write
           std-file-key (fs/file (get-null-file)))))

(defn run
  ([args] (run args {}))
  ([args {:keys [log-stdout log-stderr env]}]
   (let [p-opts (cond-> (redirect-opts log-stdout :out :out-file)
                  :always (merge (redirect-opts log-stderr :err :err-file))
                  env (assoc :extra-env env))
         binary (first args)
         user-guide-link "https://github.com/clj-commons/etaoin/blob/master/doc/01-user-guide.adoc#installing-the-browser-webdrivers"]
     (try
       (p/process args p-opts)
       (catch Throwable e
         ;; not sure if folks will see this helpful message...
         (throw (ex-info
                 (format "Failed to launch WebDriver binary `%s`.
Please ensure you have the driver installed.
If it is not on the PATH specify its location.
For driver installation, check out the Etaoin user guide: %s" binary user-guide-link)
                 {:args args} e)))))))

(defn kill
  "Ask `p` to die. Use [[result]] to get exit code if you need it."
  [p]
  (p/destroy p)
  @p)

(defn result
  "Call after killing to get result of `p`.
  If you call before killing you'll wait until p dies naturally, which a WebDriver should not do, so don't do that."
  [p]
  @p)

(defn alive?
  "Check if `p` has died unexpectedly, use [[result]] to get result."
  [p]
  (p/alive? p))
