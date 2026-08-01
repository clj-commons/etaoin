(ns etaoin.unit.error-data-test
  "Tests that thrown ex-data does not carry credentials.

  Hermetic: both code paths under test throw before any WebDriver call is made,
  so no driver and no browser are required."
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [etaoin.api :as e]
   [etaoin.impl.util :as util]
   [etaoin.test-report]))

(def ^:private secret-laden-driver
  "A driver map shaped the way the user guide tells people to build one, secrets
  and all. See doc/01-user-guide.adoc for the `browserless:token` example."
  {:type          :safari
   :host          "127.0.0.1"
   :port          4444
   :webdriver-url "https://user:sekret@ondemand.example.com/wd/hub"
   :capabilities  {"browserless:token" "TOKEN-abc123"
                   :proxy              {:httpProxy "puser:ppass@corp.proxy:8080"}}
   :env           {"SAUCE_ACCESS_KEY" "ACCESS-KEY-xyz"}})

(def ^:private secrets
  ["sekret" "TOKEN-abc123" "ppass" "ACCESS-KEY-xyz"])

(defn- leaked [ex-data-map]
  (let [dump (pr-str ex-data-map)]
    (filterv #(str/includes? dump %) secrets)))

;; ---------------------------------------------------------------------------

(deftest driver-for-report-drops-and-masks-credentials
  (let [out (util/driver-for-report secret-laden-driver)]
    (testing "credential-bearing keys are gone"
      (is (not (contains? out :capabilities)))
      (is (not (contains? out :env))))
    (testing "credentials embedded in the webdriver url are stripped"
      (is (= "https://ondemand.example.com/wd/hub" (:webdriver-url out))))
    (testing "everything else is left alone"
      (is (= :safari (:type out)))
      (is (= "127.0.0.1" (:host out)))
      (is (= 4444 (:port out))))
    (testing "no secret survives anywhere"
      (is (= [] (leaked out))))))

(deftest upload-file-of-a-missing-file-does-not-leak
  (let [exd (try
              (e/upload-file secret-laden-driver {:tag :input} "/no/such/file-xyz.txt")
              (catch Throwable ex (ex-data ex)))]
    (is (= :etaoin/file (:type exd)))
    (testing "the message still names the file"
      (is (str/includes? (:message exd) "file-xyz.txt")))
    (testing "the driver is still identifiable"
      (is (= :safari (-> exd :driver :type))))
    (testing "no secret survives anywhere in ex-data"
      (is (= [] (leaked exd))))))

(deftest print-page-on-an-unsupporting-driver-does-not-leak
  ;; :safari has no print-page support, so this hits the :default method
  (let [exd (try
              (e/print-page secret-laden-driver "out.pdf")
              (catch Throwable ex (ex-data ex)))]
    (is (= :etaoin/unsupported (:type exd)))
    (is (= :safari (-> exd :driver :type)))
    (testing "no secret survives anywhere in ex-data"
      (is (= [] (leaked exd))))))
