(ns etaoin.ide-test
  (:require [etaoin.api :as api]
            [etaoin.ide.flow :as ide]
            [clojure.test :refer :all]
            [clojure.java.io :as io]))

(def ^:dynamic *driver*)
(def ^:dynamic *base-url*)
(def ^:dynamic *test-file-path*)

(defn fixture-browser [f]
  (let [base-url       (-> "html" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (api/with-chrome {:args ["--no-sandbox"]} driver
      (api/go driver base-url)
      (binding [*driver*         driver
                *base-url*       base-url
                *test-file-path* test-file-path]
        (f)))))

(use-fixtures
  :each
  fixture-browser)

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
