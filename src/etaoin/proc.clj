(ns etaoin.proc
  (:require [clojure.java.io :as io])
  (:import java.lang.Runtime
           java.lang.IllegalThreadStateException
           java.io.IOException))

(defn java-params ^"[Ljava.lang.String;" [params]
  (->> params
       (map str)
       (into-array String)))

(defn run [args]
  (let [pb (java.lang.ProcessBuilder. (java-params args))]
    (.redirectOutput pb (java.io.File/createTempFile "driver.out" ".log"))
    (.redirectError pb (java.io.File/createTempFile "driver.err" ".log"))
    (.start pb)))

;; todo store those streams

(defn alive? [^Process proc]
  (.isAlive proc))

(defn exit-code [^Process proc]
  (try
    (.exitValue proc)
    (catch IllegalThreadStateException _)))

(defn kill [^Process proc]
  (.destroy proc))

;; todo refactor those

(defn read-out [^Process proc]
  (try
    (-> proc .getInputStream slurp)
    (catch IOException _)))

(defn read-err [^Process proc]
  (try
    (-> proc .getErrorStream slurp)
    (catch IOException _)))
