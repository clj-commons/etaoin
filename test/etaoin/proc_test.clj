(ns ^:unit etaoin.proc-test
  (:require [etaoin.api :refer :all]
            [clojure.java.shell :refer [sh]]
            [clojure.test :refer :all]
            [etaoin.proc :as proc]
            [etaoin.test-report]
            [clojure.string :as str]))

(defn get-count-chromedriver-instances
  []
  (if proc/windows?
    (->> (sh "powershell" "-command" "(Get-Process chromedriver -ErrorAction SilentlyContinue).Path")
         :out
         str/split-lines
         (remove #(str/includes? % "\\scoop\\shims\\")) ;; for the scoop users, exclude the shim process
         (filter #(str/includes? % "chromedriver"))
         count)
    (->> (sh "sh" "-c" "ps aux")
         :out
         str/split-lines
         (filter #(str/includes? % "chromedriver"))
         count)))

(deftest test-prevent-process-fork
  (testing "certain driver port"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running {:port port :host "localhost"})]
      (is (= 1 (get-count-chromedriver-instances)))
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
        (is (= 2 (get-count-chromedriver-instances)))
        (proc/kill process))))
  (testing "connect to driver"
    (let [port    9999
          process (proc/run ["chromedriver" (format "--port=%d" port)])
          _       (wait-running {:port port :host "localhost"})
          driver  (chrome {:host "localhost" :port port :args ["--no-sandbox"]})]
      (is (= 1 (get-count-chromedriver-instances)))
      (quit driver)
      (proc/kill process))))
