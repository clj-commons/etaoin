(ns etaoin.ide-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [etaoin.api :as e]
   [etaoin.ide.flow :as ide]
   [etaoin.test-report :as test-report]))

(def ^:dynamic *driver* nil)
(def ^:dynamic *base-url* nil)
(def ^:dynamic *test-file-path* nil)

(defn get-default-drivers
  "Default and supported drivers for ide tests"
  []
  [:firefox :chrome])

(defn get-drivers-from-env []
  (when-let [override (System/getenv "ETAOIN_IDE_TEST_DRIVERS")]
    (->> override
         edn/read-string
         ;; ignore drivers we can't support for these tests
         (filter #(some #{%} (get-default-drivers))))))

(def drivers
  (or (get-drivers-from-env)
      (get-default-drivers)))

(defn fixture-browser [f]
  (let [base-url       (-> "static" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (doseq [type drivers]
      (e/with-driver type driver
        (e/go driver base-url)
        (binding [*driver*         driver
                  *base-url*       base-url
                  *test-file-path* test-file-path
                  test-report/*context* (name type)]
          (testing (name type)
            (f)))))))

(use-fixtures
  :each
  fixture-browser)

(defn report-browsers [f]
  (println "Testing with browsers:" drivers)
  (f))

(use-fixtures
  :once
  report-browsers)

(deftest test-asserts
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-asserts"})
  (is 1))

(deftest test-click-type-select
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-click-type-select"})
  (is 1))

(deftest test-drag-n-drop
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-drag-n-drop"})
  (is 1))

(deftest test-select-window
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-select-window"})
  (is 1))

(deftest test-send-keys
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-send-keys"})
  (is 1))

(deftest test-control-flow
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-control-flow"})
  (is 1))

(deftest test-wait-for
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-wait-for"})
  (is 1))
