(ns webdriver.proc
  (:require [clojure.test :refer [is deftest]])
  (:import java.lang.Runtime))

(defn run [& args]
  (let [params (->> args
                    (map str)
                    into-array)
        proc (-> (Runtime/getRuntime) (.exec params))]
    proc))

(defn kill [proc]
  (-> proc .destroy))

(defn run-gecko [host port & args]
  ;;(run "/usr/local/bin/geckodriver")
  (apply run "/usr/local/bin/geckodriver" "--host" host "--port" port args)
  )
