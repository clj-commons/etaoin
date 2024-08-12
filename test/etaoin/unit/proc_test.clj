(ns etaoin.unit.proc-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [etaoin.api :as e]
   [etaoin.impl.client :as client]
   [etaoin.impl.proc :as proc]
   [etaoin.impl.util :as util]
   [etaoin.test-report])
  (:import (java.lang ProcessHandle)))

(defn all-processes []
  (for [p (-> (ProcessHandle/allProcesses) .iterator iterator-seq)
        :when (some-> p .info .command .isPresent)
        :let [info (.info p)
              command (-> info .command .get)
              arguments (when (-> info .arguments .isPresent)
                          (->> info .arguments .get (into [])))
              start-instant (-> info .startInstant .get)]]
    {:pid (.pid p)
     :is-alive (.isAlive p)
     :start-instant start-instant
     :handle p
     :command command
     :arguments arguments}))

(defn get-count-driver-instances
  [drivername]
  (->> (all-processes)
       (remove #(str/includes? (:command %) "\\scoop\\shims\\")) ;; exclude windows scoop shims
       (filter #(str/includes? (:command %) drivername))
       count)

  #_(if proc/windows?
    (let [instance-report (-> (shell/sh "powershell" "-command" (format "(Get-Process %s -ErrorAction SilentlyContinue).Path" drivername))
                              :out
                              str/split-lines)]
      (->> instance-report
           (remove #(str/includes? % "\\scoop\\shims\\")) ;; for the scoop users, exclude the shim process
           (filter #(str/includes? % drivername))
           count))
    (->> (shell/sh "sh" "-c" "ps aux")
         :out
         str/split-lines
         (filter #(str/includes? % drivername))
         count)))

(defn get-count-chromedriver-instances []
  (get-count-driver-instances "chromedriver"))

(defn get-count-firefoxdriver-instances []
  (get-count-driver-instances "geckodriver"))

(deftest test-process-forking-port-specified-is-in-use
  (let [port    (util/get-free-port)
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (is (= 1 (get-count-chromedriver-instances)))
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"already in use"
            (e/chrome {:port port})))
      (finally
        (proc/kill process)))))

(deftest test-process-forking-port-not-specified-so-random-port-is-picked
  (let [port    (util/get-free-port)
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (e/with-chrome driver
        ;; added to diagnose flakyness on windows on CI
        (println "automatically chosen port->" (:port driver))
        ;; added to diagnose flakyness on windows on CI
        (e/wait-running driver)
        (is (= 2 (get-count-chromedriver-instances))))
      (finally
        (proc/kill process)))))

(deftest test-process-forking-connect-existing-webdriver-host
  (let [port    (util/get-free-port)
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      ;; should connect, not launch
      (let [driver (e/chrome {:host "localhost" :port port})]
        (is (= 1 (get-count-chromedriver-instances)))
        (e/quit driver))
      (finally
        (proc/kill process)))))

(deftest test-process-forking-connect-existing-webdriver-url
  (let [port    (util/get-free-port)
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (let [;; should connect, not launch
            driver  (e/chrome {:webdriver-url (format "http://localhost:%d" port)})]


        (is (= 1 (get-count-chromedriver-instances)))
        (e/quit driver))
      (finally
        (proc/kill process)))))

(deftest http-exception-on-create-proc-alive
  ;; when etaoin tries to create a session we simulate a read timeout
  (with-redefs [client/http-request (fn [_] (throw (ex-info "read timeout" {})))]
    (let [ex (try
               (e/with-firefox _driver
                 (is false "should not reach here"))
               (catch Throwable ex
                 {:exception ex}))
          exd (-> ex :exception ex-data)]
      (is (= :etaoin/http-ex (:type exd)))
      (is (= nil (-> exd :driver :process :exit)))
      (is (= 0 (get-count-firefoxdriver-instances))))))

(deftest http-error-on-create-proc-alive
  ;; when etaoin tries to create a session return an http error
  (with-redefs [client/http-request (fn [_] {:status 400})]
    (let [ex (try
               (e/with-firefox _driver
                 (is false "should not reach here"))
               (catch Throwable ex
                 {:exception ex}))
          exd (-> ex :exception ex-data)]
      (is (= :etaoin/http-error (:type exd)))
      (is (= 400 (:status exd)))
      (is (= nil (-> exd :driver :process :exit)))
      (is (= 0 (get-count-firefoxdriver-instances))))))

(deftest http-exception-after-create-proc-now-dead
  (let [orig-http-request client/http-request]
    (with-redefs [client/http-request (fn [{:keys [method uri] :as params}]
                                        ;; allow create session through, fail on everything else
                                        (if (and (= :post method) (str/ends-with? uri "/session"))
                                          (orig-http-request params)
                                          (throw (ex-info "read timeout" {}))))]
      (let [ex (try
                 ;; go headless to avoid that dangling open web browser
                 (e/with-firefox-headless driver
                   (is (= 1 (get-count-firefoxdriver-instances)))
                   (proc/kill (:process driver))
                   ;; we'll now fail on this call
                   (e/go driver "https://clojure.org"))
                 (catch Throwable ex
                   {:exception ex}))
            exd (-> ex :exception ex-data)]
        (is (= :etaoin/http-ex (:type exd)))
        ;; actual exit code varies by OS and is not important for this test
        (is (integer? (-> exd :driver :process :exit)))
        (is (= 0 (get-count-firefoxdriver-instances)))))))

(deftest http-error-after-create-proc-now-dead
  ;; unlikely, we know we just talked to the driver because it returned an http error, but for completeness
  (let [orig-http-request client/http-request]
    (with-redefs [client/http-request (fn [{:keys [method uri] :as params}]
                                        ;; allow create session through, fail on everything else
                                        (if (and (= :post method) (str/ends-with? uri "/session"))
                                          (orig-http-request params)
                                          {:status 418}))]
      (let [ex (try
                 ;; go headless to avoid that dangling open web browser
                 (e/with-firefox-headless driver
                   (is (= 1 (get-count-firefoxdriver-instances)))
                   (proc/kill (:process driver))
                   ;; we'll now fail on this call
                   (e/go driver "https://clojure.org"))
                 (catch Throwable ex
                   {:exception ex}))
            exd (-> ex :exception ex-data)]
        (is (= :etaoin/http-error (:type exd)))
        (is (= 418 (:status exd)))
        ;; actual exit code varies by OS and is not important for this test
        (is (integer? (-> exd :driver :process :exit)))
        (is (= 0 (get-count-firefoxdriver-instances)))))))

(deftest test-cleanup-connect-existing-on-create-error
  (let [port    (util/get-free-port)
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (with-redefs [client/http-request (fn [_] (throw (ex-info "read timeout" {})))]
        ;; attempt connect, not launch
        (let [ex (try
                   (e/with-chrome {:webdriver-url (format "http://localhost:%d" port)} _driver
                     (is false "should not get here"))
                   (catch Throwable ex
                     {:exception ex}))
              exd (-> ex :exception ex-data)]
          (is (= :etaoin/http-ex (:type exd)))
          (is (= 1 (get-count-chromedriver-instances)))))
      (finally
        (proc/kill process)))))
