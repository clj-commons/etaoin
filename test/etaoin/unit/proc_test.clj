(ns etaoin.unit.proc-test
  (:require
   [clojure.java.shell :as shell]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [etaoin.api :as e]
   [etaoin.impl.client :as client]
   [etaoin.impl.proc :as proc]
   [etaoin.test-report]))

(defn get-count-driver-instances
  [drivername]
  (if proc/windows?
    (let [instance-report (-> (shell/sh "powershell" "-command" (format "(Get-Process %s -ErrorAction SilentlyContinue).Path" drivername))
                              :out
                              str/split-lines)]
      ;; more flakiness diagnosis
      (println "windows" drivername "instance report:" instance-report)
      (println "windows full list of running processes:")
      ;; use Get-CimInstance, because Get-Process, does not have commandline available
      (pprint/pprint (-> (shell/sh "powershell" "-command" "Get-CimInstance Win32_Process | select name, commandline")
                         :out
                         str/split-lines))
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
  (let [port    9997
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
  (let [port    9998
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (e/with-chrome {:args ["--no-sandbox"]} driver
        ;; added to diagnose flakyness on windows on CI
        (println "automatically chosen port->" (:port driver))
        ;; added to diagnose flakyness on windows on CI
        (e/wait-running driver)
        (is (= 2 (get-count-chromedriver-instances))))
      (finally
        (proc/kill process)))))

(deftest test-process-forking-connect-existing-webdriver-host
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      ;; should connect, not launch
      (let [driver (e/chrome {:host "localhost" :port port :args ["--no-sandbox"]})]
        (is (= 1 (get-count-chromedriver-instances)))
        (e/quit driver))
      (finally
        (proc/kill process)))))

(deftest test-process-forking-connect-existing-webdriver-url
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (let [;; should connect, not launch
            driver  (e/chrome {:webdriver-url (format "http://localhost:%d" port) :args ["--no-sandbox"]})]


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
    (with-redefs [client/http-request (fn [{:keys [method url] :as params}]
                                        ;; allow create session through, fail on everything else
                                        (if (and (= :post method) (str/ends-with? url "/session"))
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
        (is (= 143 (-> exd :driver :process :exit)))
        (is (= 0 (get-count-firefoxdriver-instances)))))))

(deftest http-error-after-create-proc-now-dead
  ;; unlikely, we know we just talked to the driver because it returned an http error, but for completeness
  (let [orig-http-request client/http-request]
    (with-redefs [client/http-request (fn [{:keys [method url] :as params}]
                                        ;; allow create session through, fail on everything else
                                        (if (and (= :post method) (str/ends-with? url "/session"))
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
        (is (= 143 (-> exd :driver :process :exit)))
        (is (= 0 (get-count-firefoxdriver-instances)))))))

(deftest test-cleanup-connect-existing-on-create-error
  (let [port    9999
        process (proc/run ["chromedriver" (format "--port=%d" port)])]
    (try
      (e/wait-running {:port port :host "localhost"})
      (with-redefs [client/http-request (fn [_] (throw (ex-info "read timeout" {})))]
        ;; attempt connect, not launch
        (let [ex (try
                   (e/with-chrome {:webdriver-url (format "http://localhost:%d" port) :args ["--no-sandbox"]} driver
                     (is false "should not get here"))
                   (catch Throwable ex
                     {:exception ex}))
              exd (-> ex :exception ex-data)]
          (is (= :etaoin/http-ex (:type exd)))
          (is (= 1 (get-count-chromedriver-instances)))))
      (finally
        (proc/kill process)))))
