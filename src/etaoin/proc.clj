(ns etaoin.proc
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def windows? (str/starts-with? (System/getProperty "os.name") "Windows"))

(defn- get-null-file ^java.io.File
  []
  (if windows?
    (io/file "NUL")
    (io/file "/dev/null")))

(defn- get-log-file ^java.io.File
  [file-path]
  (if file-path
    (io/file file-path)
    (get-null-file)))

(defn- java-params ^"[Ljava.lang.String;" [params]
  (->> params
       (map str)
       (into-array String)))

(defn run
  ([args] (run args {}))
  ([args {:keys [log-stdout log-stderr env]}]
   (let [binary      (first args)
         readme-link "https://github.com/clj-commons/etaoin#installing-the-browser-drivers"
         pb          (java.lang.ProcessBuilder. (java-params args))
         pb-env      (.environment pb)]
     (when env
       (doseq [[k v] env]
         (.put pb-env (name k) (str v))))
     (.redirectOutput pb (get-log-file log-stdout))
     (.redirectError pb  (get-log-file log-stderr))
     (try
       (.start pb)
       (catch java.io.IOException e
         (throw (ex-info
                  (format "Cannot find a binary file `%s` for the driver.
Please ensure you have the driver installed and specify the path to it.
For driver installation, check out the official readme file from Etaoin: %s" binary readme-link)
                  {:args args} e)))))))

(defn kill [^Process proc]
  (.destroy proc))

