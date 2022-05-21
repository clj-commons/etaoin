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
    (let [instance-report (-> (sh "powershell" "-command" "(Get-Process chromedriver -ErrorAction SilentlyContinue).Path")
                              :out
                              str/split-lines)]
      ;; more flakiness diagnosis
      (println "windows chromedriver instance report:" instance-report)
      (println "windows full list of running processes:")
      (clojure.pprint/pprint (-> (sh "powershell" "-command" "Get-Process | Select Name, Path")
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
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (wait-running {:port port :host "localhost"})]
    (is (= 1 (get-count-chromedriver-instances)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"already in use"
         (chrome {:port port})))
    (proc/kill process)))

(deftest test-process-forking-port-random
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (wait-running {:port port :host "localhost"})]
    (with-chrome {:args ["--no-sandbox"]} driver
        ;; added to diagnose flakyness on windows on CI
      (println "automatically chosen port->" (:port driver))
        ;; added to diagnose flakyness on windows on CI
      (wait-running {:port (:port driver) :host "localhost"})
      (is (= 2 (get-count-chromedriver-instances))))
    (proc/kill process)))

(deftest test-process-forking-connect-existing
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])
        _       (wait-running {:port port :host "localhost"})
        driver  (chrome {:host "localhost" :port port :args ["--no-sandbox"]})]
    (is (= 1 (get-count-chromedriver-instances)))
    (quit driver)
    (proc/kill process)))
