(ns etaoin.ide-test
  (:require [etaoin.api :as api]
            [etaoin.ide :as ide]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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
  (let [url          (api/get-url *driver*)
        selected-val (api/get-element-value *driver* :simple-country)]
    (is  (str/ends-with? url "?login=1&password=2&message=3"))
    (is (= "usa" selected-val))))

(deftest test-drag-n-drop
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-drag-n-drop"})
  (is (api/absent? *driver* {:class :document})))

(deftest test-select-window
  (let [init-handle  (api/get-window-handle *driver*)
        _            (ide/run-ide-script *driver* *test-file-path*
                                         {:base-url *base-url* :test-name "test-select-window"})
        final-handle (api/get-window-handle *driver*)]
    (is (not= init-handle final-handle))))

(deftest test-send-keys
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-send-keys"})
  (let [url (api/get-url *driver*)]
    (is  (str/ends-with? url "login=LOGin&password=3*3%3D9&message="))))

(deftest test-control-flow
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-control-flow"})
  (is 1))

(deftest test-wait-for
  (ide/run-ide-script *driver* *test-file-path*
                      {:base-url *base-url* :test-name "test-wait-for"})
  (is 1))
