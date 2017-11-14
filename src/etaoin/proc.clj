(ns etaoin.proc
  (:require [clojure.java.io :as io])
  (:import java.lang.Runtime
           java.lang.IllegalThreadStateException
           java.io.IOException))

(defn java-params [params]
  (->> params
       (map str)
       (into-array String)))

(defn run [args]
  (let [pb (java.lang.ProcessBuilder. (java-params args))]
    (.redirectOutput pb (java.io.File/createTempFile "driver.out" ".log"))
    (.redirectError pb (java.io.File/createTempFile "driver.err" ".log"))
    (.start pb)))

;; todo store those streams

(defn alive? [proc]
  (-> proc .isAlive))

(defn exit-code [proc]
  (try
    (-> proc .exitValue)
    (catch IllegalThreadStateException _)))

(defn kill [proc]
  (-> proc .destroy))

;; todo refactor those

(defn read-out [proc]
  (try
    (-> proc .getInputStream slurp)
    (catch IOException _)))

(defn read-err [proc]
  (try
    (-> proc .getErrorStream slurp)
    (catch IOException _)))
