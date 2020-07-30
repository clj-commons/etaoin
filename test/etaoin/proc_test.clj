(ns etaoin.proc-test
  (:require [etaoin.proc :as proc]
            [clojure.test :as t]))


(t/deftest test-fail-run-driver
  (let [args '("wrong-driver-path" "--help")]
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"wrong-driver-path"
          (proc/run args)))))
