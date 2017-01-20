(ns webdriver.proc
  (:require [clojure.test :refer [is deftest]]
            [clojure.java.io :as io])
  (:import java.lang.Runtime
           java.lang.IllegalThreadStateException))

(defmulti run2 (fn [& attrs] (mapv class attrs)))

(defmethod run2 [clojure.lang.PersistentVector & foo] [params]
  (print 42))

;; (defmethod run [clojure.lang.PersistentVector APersistentMap]
;;   )


(defn run3

  ;; ([cmd]
  ;;  (run cmd ))
  ;; ([cmd params]
  ;;  (run cmd []))

  [cmd params env dir]
  (let [java-params (->> params
                         (cons cmd)
                         (map str)
                         (into-array String))
        env-pair (fn [[k v]] (format "%s=%s" (name k) v))
        java-env (->> env (map env-pair) (into-array String))
        java-file (io/file dir)
        runtime (Runtime/getRuntime)]
    (.exec runtime
           java-params
           java-env
           java-file)))

(defn alive? [proc]
  (-> proc .isAlive))

(defn exit-code [proc]
  (try
    (-> proc .exitValue)
    (catch IllegalThreadStateException _)))

(defn kill [proc]
  (-> proc .destroy))

(defn run-server [command host port & args]
  )

(defn run-gecko [host port & args]
  (apply run "geckodriver" "--host" host "--port" port args))
