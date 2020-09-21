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
      (ide/run-ide-script driver test-file-path {:base-url base-url :test-name "test-click-type-select"})
      (let [url          (api/get-url driver)
            selected-val (api/get-element-value driver :simple-country)]
        (is  (str/ends-with? url "?login=1&password=2&message=3"))
        (is (= "usa" selected-val))))))
