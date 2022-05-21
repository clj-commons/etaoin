(ns ^:unit etaoin.proc-test
  (:require [etaoin.api :refer :all]
            [clojure.java.shell :refer [sh]]
            [clojure.test :refer :all]
            [etaoin.proc :as proc]
            [etaoin.test-report]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(defn get-count-chromedriver-instances
  []
  (if proc/windows?
    (let [instance-report (-> (sh "powershell" "-command" "(Get-Process chromedriver -ErrorAction SilentlyContinue).Path")
                              :out
                              str/split-lines)]
      ;; more flakiness diagnosis
      (println "windows chromedriver instance report:" instance-report)
      (println "windows full list of running processes:")
      ;; use Get-CimInstance, because Get-Process, does not have commandline available
      (pprint/pprint (-> (sh "powershell" "-command" "Get-CimInstance Win32_Process | select name, commandline")
                         :out
                         str/split-lines))
      (->> instance-report
           (remove #(str/includes? % "\\scoop\\shims\\")) ;; for the scoop users, exclude the shim process
           (filter #(str/includes? % "chromedriver"))
           count))
    (->> (sh "sh" "-c" "ps aux")
         :out
         str/split-lines
         (filter #(str/includes? % "chromedriver"))
         count)))

(deftest test-process-forking-port-specified
  (let [port    9997
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (wait-running {:port port :host "localhost"})]
    (is (= 1 (get-count-chromedriver-instances)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"already in use"
         (chrome {:port port})))
    (proc/kill process)))

(deftest test-process-forking-port-random
  (let [port    9998
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (wait-running {:port port :host "localhost"})]
    (with-chrome {:args ["--no-sandbox"]} driver
        ;; added to diagnose flakyness on windows on CI
      (println "automatically chosen port->" (:port driver))
        ;; added to diagnose flakyness on windows on CI
      (wait-running driver)
      (is (= 2 (get-count-chromedriver-instances))))
    (proc/kill process)))

(deftest test-process-forking-connect-existing
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (wait-running {:port port :host "localhost"})
        driver  (chrome {:host "localhost" :port port :args ["--no-sandbox"]})]
    (wait-running driver)
    (is (= 1 (get-count-chromedriver-instances)))
    (quit driver)
    (proc/kill process)))
