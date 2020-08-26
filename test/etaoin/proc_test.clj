(ns etaoin.proc-test
  (:require [etaoin.api :refer :all]
            [clojure.java.shell :refer [sh]]
            [clojure.test :refer :all]
            [etaoin.proc :as proc]
            [clojure.string :as str]))

(defn get-count-chromedriver-processes
  []
  (->> (sh "sh" "-c" "ps aux | grep chromedriver")
       :out
       str/split-lines
       (drop-last 2)
       count))

(deftest test-prevent-process-fork
  (testing "certain driver port"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running {:port port :host "localhost"})]
      (is (= 1 (get-count-chromedriver-processes)))
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"already in use"
            (chrome {:port port})))
      (proc/kill process)))
  (testing "random driver port"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running {:port port :host "localhost"})]
      (with-chrome {:args ["--no-sandbox"]} driver
        (is (= 2 (get-count-chromedriver-processes)))
        (proc/kill process))))
  (testing "connect to driver"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running {:port port :host "localhost"})
          driver  (chrome {:host "localhost" :port port :args ["--no-sandbox"]})]
      (is (= 1 (get-count-chromedriver-processes)))
      (proc/kill process))))
