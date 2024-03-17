(ns etaoin.api-with-driver-test
  "Make sure all this sugar works and is linted by clj-kondo appropriately.

  These tests are a bit wordy and code is repeated, but it all seems appropriate to me at this time.

  These tests are separate etaoin.api-test because it uses a fixture as part of its strategy.
  We do reuse the driver selection mechanism from etaoin.api-test tho."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [etaoin.api :as e]
            [etaoin.api2 :as e2]
            [etaoin.api-test :as api-test]
            [etaoin.test-report]))

(defn testing-driver? [type]
  (some #{type} api-test/drivers))

(def my-agent "my agent")

(use-fixtures
  :once
  api-test/test-server)

(deftest with-driver-tests
  (let [test-page (api-test/test-server-url "test.html")]
    (when (testing-driver? :chrome)
      (testing "chrome"
        (println "testing chrome")
        ;; with opts
        (is (= my-agent
               (e/with-driver :chrome {:user-agent my-agent} driver
                 (e/get-user-agent driver))
               (e/with-chrome {:user-agent my-agent} driver
                 (e/get-user-agent driver))
               (e/with-chrome-headless {:user-agent my-agent} driver
                 (is (= true (e/headless? driver)))
                 (e/get-user-agent driver))
               (e2/with-chrome [driver {:user-agent my-agent}]
                 (e/get-user-agent driver))
               (e2/with-chrome-headless [driver {:user-agent my-agent}]
                 (is (= true (e/headless? driver)))
                 (e/get-user-agent driver))))
        ;; without opts
        (is (= "Webdriver Test Document"
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-driver :chrome nil driver
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-driver :chrome {} driver
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-driver :chrome driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-chrome nil driver
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-chrome {} driver
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-chrome driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-chrome-headless nil driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-chrome-headless {} driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-chrome-headless driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))

               (e2/with-chrome [driver]
                 (e/go driver test-page)
                 (e/get-title driver))
               (e2/with-chrome-headless [driver]
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))))))

    (when (testing-driver? :firefox)
      (testing "firefox"
        (println "testing firefox")
        ;; with opts
        (is (= my-agent
               (e/with-driver :firefox {:user-agent my-agent} driver
                 (e/get-user-agent driver))
               (e/with-firefox {:user-agent my-agent} driver
                 (e/get-user-agent driver))
               (e/with-firefox-headless {:user-agent my-agent} driver
                 (is (= true (e/headless? driver)))
                 (e/get-user-agent driver))
               (e2/with-firefox [driver {:user-agent my-agent}]
                 (e/get-user-agent driver))
               (e2/with-firefox-headless [driver {:user-agent my-agent}]
                 (is (= true (e/headless? driver)))
                 (e/get-user-agent driver))))
        ;; without opts
        (is (= "Webdriver Test Document"
               (e/with-driver :firefox driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-firefox nil driver
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-firefox {} driver
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-firefox driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-firefox-headless nil driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-firefox-headless {} driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-firefox-headless driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))

               (e2/with-firefox [driver]
                 (e/go driver test-page)
                 (e/get-title driver))
               (e2/with-firefox-headless [driver]
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))))))

    (when (testing-driver? :edge)
      (testing "edge"
        (println "testing edge")
        ;; with opts
        (is (= my-agent
               (e/with-driver :edge {:user-agent my-agent} driver
                 (e/get-user-agent driver))
               (e/with-edge {:user-agent my-agent} driver
                 (e/get-user-agent driver))
               (e/with-edge-headless {:user-agent my-agent} driver
                 (is (= true (e/headless? driver)))
                 (e/get-user-agent driver))
               (e2/with-edge [driver {:user-agent my-agent}]
                 (e/get-user-agent driver))
               (e2/with-edge-headless [driver {:user-agent my-agent}]
                 (is (= true (e/headless? driver)))
                 (e/get-user-agent driver))))
        ;; without opts
        (is (= "Webdriver Test Document"
               (e/with-driver :edge driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-edge nil driver
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-edge {} driver
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-edge driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-edge-headless nil driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-edge-headless {} driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-edge-headless driver
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))

               (e2/with-edge [driver]
                 (e/go driver test-page)
                 (e/get-title driver))
               (e2/with-edge-headless [driver]
                 (is (= true (e/headless? driver)))
                 (e/go driver test-page)
                 (e/get-title driver))))))

    (when (testing-driver? :safari)
      (testing "safari"
        (println "testing safari")
        ;; with opts
        ;; safari driver does supports neither user agent nor headless
        ;; not sure what other safari option is reflected in session... port?
        (let [port 9995]
          (e/with-driver :safari {:port port} driver
            (is (= port (:port driver)))
            (is (= true (e/running? driver))))
          (e/with-safari {:port port} driver
            (is (= port (:port driver)))
            (is (= true (e/running? driver))) )
          (e2/with-safari [driver {:port port}]
            (is (= port (:port driver)))
            (is (= true (e/running? driver)))))
        ;; without opts
        (is (= "Webdriver Test Document"
               (e/with-driver :safari driver
                 (e/go driver test-page)
                 (e/get-title driver))

               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-safari nil driver
                 (e/go driver test-page)
                 (e/get-title driver))
               #_{:clj-kondo/ignore [:etaoin/empty-opts]}
               (e/with-safari {} driver
                 (e/go driver test-page)
                 (e/get-title driver))
               (e/with-safari driver
                 (e/go driver test-page)
                 (e/get-title driver))

               (e2/with-safari [driver]
                 (e/go driver test-page)
                 (e/get-title driver))))))))
