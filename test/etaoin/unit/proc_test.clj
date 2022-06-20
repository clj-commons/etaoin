(ns etaoin.unit.proc-test
  (:require
   [clojure.java.shell :as shell]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [etaoin.api :as e]
   [etaoin.impl.proc :as proc]
   [etaoin.test-report]))

(defn get-count-chromedriver-instances
  []
  (if proc/windows?
    (let [instance-report (-> (shell/sh "powershell" "-command" "(Get-Process chromedriver -ErrorAction SilentlyContinue).Path")
                              :out
                              str/split-lines)]
      ;; more flakiness diagnosis
      (println "windows chromedriver instance report:" instance-report)
      (println "windows full list of running processes:")
      ;; use Get-CimInstance, because Get-Process, does not have commandline available
      (pprint/pprint (-> (shell/sh "powershell" "-command" "Get-CimInstance Win32_Process | select name, commandline")
                         :out
                         str/split-lines))
      (->> instance-report
           (remove #(str/includes? % "\\scoop\\shims\\")) ;; for the scoop users, exclude the shim process
           (filter #(str/includes? % "chromedriver"))
           count))
    (->> (shell/sh "sh" "-c" "ps aux")
         :out
         str/split-lines
         (filter #(str/includes? % "chromedriver"))
         count)))

(deftest test-process-forking-port-specified
  (let [port    9997
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (e/wait-running {:port port :host "localhost"})]
    (is (= 1 (get-count-chromedriver-instances)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"already in use"
         (e/chrome {:port port})))
    (proc/kill process)))

(deftest test-process-forking-port-random
  (let [port    9998
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (e/wait-running {:port port :host "localhost"})]
    (e/with-chrome {:args ["--no-sandbox"]} driver
      ;; added to diagnose flakyness on windows on CI
      (println "automatically chosen port->" (:port driver))
      ;; added to diagnose flakyness on windows on CI
      (e/wait-running driver)
      (is (= 2 (get-count-chromedriver-instances))))
    (proc/kill process)))

(deftest test-process-forking-connect-existing-host
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (e/wait-running {:port port :host "localhost"})
        ;; should connect, not launch
        driver  (e/chrome {:host "localhost" :port port :args ["--no-sandbox"]})]
    (is (= 1 (get-count-chromedriver-instances)))
    (e/quit driver)
    (proc/kill process)))

(deftest test-process-forking-connect-existing-webdriver-url
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        ;; normally would not call wait-running for a remote service, we are simulating here and want
        ;; to make sure the process we launched is up and running
        _       (e/wait-running {:port port :host "localhost"})
        ;; should connect, not launch
        driver  (e/chrome {:webdriver-url (format "http://localhost:%d" port) :args ["--no-sandbox"]})]
    (is (= 1 (get-count-chromedriver-instances)))
    (e/quit driver)
    (proc/kill process)))
