(ns webdriver.proc
  (:require [clojure.java.io :as io])
  (:import java.lang.Runtime
           java.lang.IllegalThreadStateException))

(defmacro exec [& args]
  `(.exec (Runtime/getRuntime) ~@args))

(defn java-params [params]
  (->> params
       (map str)
       (into-array String)))

(defn java-env [env]
  (->> env
       (map (fn [[k v]]
              (format "%s=%s" (name k) v)))
       (into-array String)))

(defn run
  ([params]
   (exec (java-params params)))
  ([params env]
   (exec (java-params params) (java-env env)))
  ([params env dir]
   (exec (java-params params) (java-env env) (io/file dir))))

(defn alive? [proc]
  (-> proc .isAlive))

(defn exit-code [proc]
  (try
    (-> proc .exitValue)
    (catch IllegalThreadStateException _)))

(defn kill [proc]
  (-> proc .destroy))

(defn read-out [proc]
  (-> proc .getInputStream slurp))

(defn read-err [proc]
  (-> proc .getErrorStream slurp))

(defmacro with-proc [proc args & body]
  `(let [~proc (apply run ~args)]
     (try
       ~@body
       (finally
         (kill ~proc)))))
