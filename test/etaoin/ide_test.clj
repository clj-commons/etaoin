(ns etaoin.ide-test
  (:require [etaoin.api :as api]
            [etaoin.ide :as ide]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]))


(deftest test-click-type-select
  (let [base-url       (-> "html" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (api/with-chrome {:args ["--no-sandbox"]} driver
      (ide/run-ide-script driver test-file-path
                          {:base-url base-url :test-name "test-click-type-select"})
      (let [url          (api/get-url driver)
            selected-val (api/get-element-value driver :simple-country)]
        (is  (str/ends-with? url "?login=1&password=2&message=3"))
        (is (= "usa" selected-val))))))

(deftest test-drag-n-drop
  (let [base-url       (-> "html" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (api/with-chrome {:args ["--no-sandbox"]} driver
      (ide/run-ide-script driver test-file-path
                          {:base-url base-url :test-name "test-drag-n-drop"})
      (is (api/absent? driver {:class :document})))))

(deftest test-select-window
  (let [base-url       (-> "html" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (api/with-chrome {:args ["--no-sandbox"]} driver
      (let [init-handle  (api/get-window-handle driver)
            _            (ide/run-ide-script driver test-file-path
                                             {:base-url base-url :test-name "test-select-window"})
            final-handle (api/get-window-handle driver)]
        (is (not= init-handle final-handle))))))

(deftest test-send-keys
  (let [base-url       (-> "html" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (api/with-chrome {:args ["--no-sandbox"]} driver
      (ide/run-ide-script driver test-file-path
                          {:base-url base-url :test-name "test-send-keys"})
      (let [url (api/get-url driver)]
        (is  (str/ends-with? url "login=LOGin&password=3*3%3D9&message="))))))

(deftest test-control-flow
  (let [base-url       (-> "html" io/resource str)
        test-file-path (-> "ide/test.side" io/resource str)]
    (api/with-chrome {:args ["--no-sandbox"]} driver
      (ide/run-ide-script driver test-file-path
                          {:base-url base-url :test-name "test-control-flow"})
      (is 1))))
