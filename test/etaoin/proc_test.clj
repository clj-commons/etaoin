(ns etaoin.proc-test
  (:require [etaoin.api :refer :all]
            [clojure.java.shell :refer [sh]]
            [clojure.test :refer :all]
            [etaoin.proc :as proc]))

(deftest test-prevent-process-fork
  (testing "certain driver port"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running (atom {:port port :host "localhost"}))]
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"already in use"
            (chrome {:port port})))
      (proc/kill process)))
  (testing "random driver port"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running (atom {:port port :host "localhost"}))]
      (with-chrome {:args ["--no-sandbox"]} driver
        (is true "driver runnig")
        (proc/kill process)))))
