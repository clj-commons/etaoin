(ns webdriver.proc
  (:require [clojure.test :refer [is deftest]])
  (:import java.lang.Runtime
           java.lang.IllegalThreadStateException))

(defn run [& args]
  (let [params (->> args (map str) into-array)]
    (-> (Runtime/getRuntime) (.exec params))))

(defn alive? [proc]
  (-> proc .isAlive))

(defn exit-code [proc]
  (try
    (-> proc .exitValue)
    (catch IllegalThreadStateException _)))

(defn kill [proc]
  (-> proc .destroy))

(defn run-gecko [host port & args]
  ;;(run "/usr/local/bin/geckodriver")
  (apply run "geckodriver" "--host" host "--port" port args)
  )
