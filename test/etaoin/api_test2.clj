(ns etaoin.api-test2
  (:require [etaoin.api :refer :all]
            [etaoin.proc]
            [clojure.test :refer :all]))

#_(deftest test-firefox-driver-args
    (let [args (atom [])]
      (with-redefs-fn {#'etaoin.proc/run (fn [a _] (reset! args a))}
        #(do (testing "No custom args"
               (-> (create-driver :firefox {:port 1234})
                   (run-driver {}))
               (is (= @args
                      ["geckodriver" "--port" 1234])))
             (testing "Default `--marionette-port` is assigned when `:profile` is specified"
               (-> (create-driver :firefox {:port 1234})
                   (run-driver {:profile "/tmp/firefox-profile/1"}))
               (is (= @args
                      ["geckodriver" "--port" 1234 "--marionette-port" 2828])))
             (testing "Custom `--marionette-port` is assigned when `:profile` is specified"
               (-> (create-driver :firefox {:port 1234})
                   (run-driver {:profile     "/tmp/firefox-profile/1"
                                :args-driver ["--marionette-port" 2821]}))
               (is (= @args
                      ["geckodriver" "--port" 1234 "--marionette-port" 2821])))))))


(deftest test-fail-run-driver
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"wrong-driver-path"
        (chrome {:path-driver "wrong-driver-path"}))))
