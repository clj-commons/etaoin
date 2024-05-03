(ns etaoin.unit.unit-test
  (:require
   [babashka.fs :as fs]
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [etaoin.api :as e]
   [etaoin.ide.flow :as ide]
   [etaoin.ide.impl.spec :as spec]
   [etaoin.impl.proc :as proc]
   [etaoin.impl.util :as util]
   [etaoin.test-report])
  (:import [clojure.lang ExceptionInfo]))

(deftest test-driver-options
  (with-redefs
   [etaoin.impl.proc/run  (fn [_ _] {:some :process})
    e/wait-running     identity
    e/create-session   (fn [_ _] "session-key")
    proc/kill identity
    e/delete-session   identity
    util/get-free-port (constantly 12345)]
    (testing "defaults"
      (e/with-firefox driver
        (is (= {:args ["geckodriver" "--port" 12345]
                :capabilities {:loggingPrefs {:browser "ALL"}}
                :host "127.0.0.1"
                :locator "xpath"
                :port 12345
                :process {:some :process}
                :session "session-key"
                :type :firefox
                :url "http://127.0.0.1:12345"} driver))))
    (testing "port"
      (e/with-firefox {:port 1234} driver
        (is (= ["geckodriver" "--port" 1234]
               (:args driver)))))
    (testing "when host, default to port for driver, process options are not relevant"
      (e/with-firefox {:host "somehost"} driver
        (is (= {:host "somehost"
                :locator "xpath"
                :port 4444
                :session "session-key"
                :type :firefox
                :url "http://somehost:4444"} driver))))
    (testing "default `--marionette-port` is assigned when `:profile` is specified"
      (e/with-firefox {:port 1234 :profile "/tmp/firefox-profile/1"} driver
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2828]
               (:args driver)))))
    (testing "custom `--marionette-port` is assigned when `:profile` is specified"
      (e/with-firefox {:port 1234
                       :profile     "/tmp/firefox-profile/1"
                       :args-driver ["--marionette-port" 2821]} driver
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2821]
               (:args driver)))))
    (testing "capabilities are deep merged"
      (with-redefs
       [e/defaults-global (assoc e/defaults-global :capabilities {:default-firefox :val1
                                                                  :some {:deeper {:thing0 0
                                                                                  :thing1 1
                                                                                  :thing2 2
                                                                                  :thing3 3}}})
        e/defaults (assoc-in e/defaults [:firefox :capabilities] {:default-global :val2
                                                                  :some {:deeper {:thing0 10
                                                                                  :thing3 30
                                                                                  :thing4 40}}})]
        (e/with-firefox {:capabilities {:specified :val2
                                        :some {:deeper {:thing1 100
                                                        :thing3 300
                                                        :thing5 500}}}
                         :desired-capabilities {:specified-desired :val3
                                                :some {:deeper {:thing3 3000
                                                                :thing6 6000}}}} driver
          (is (= {:args ["geckodriver" "--port" 12345],
                  :capabilities
                  {:default-firefox :val1
                   :default-global :val2
                   :loggingPrefs {:browser "ALL"},
                   :some {:deeper {:thing0 10
                                   :thing1 100
                                   :thing2 2
                                   :thing3 3000
                                   :thing4 40
                                   :thing5 500
                                   :thing6 6000}}
                   :specified :val2
                   :specified-desired :val3}
                  :host "127.0.0.1"
                  :locator "xpath"
                  :port 12345
                  :process {:some :process}
                  :session "session-key"
                  :type :firefox
                  :url "http://127.0.0.1:12345"} driver)))))))

(deftest test-chrome-profile
  (fs/with-temp-dir [chrome-dir {:prefix "chrome-dir"}]
    (let [profile-path (str (fs/file chrome-dir "chrome-profile"))]
      (e/with-chrome {:profile profile-path :args ["--no-sandbox"]} driver
        (e/go driver "chrome://version")
        (is profile-path
            (e/get-element-text driver :profile_path))))))

(deftest test-fail-run-driver
  (is (thrown-with-msg?
        ExceptionInfo
        #"wrong-driver-path"
        (e/chrome {:path-driver "wrong-driver-path"}))))

(deftest test-retry-launch
  (testing "give up after max tries"
    (let [run-calls (atom 0)
          warnings-logged (atom [])
          ex (ex-info "firefox badness" {})]
      (with-redefs
        [etaoin.impl.proc/run  (fn [_ _]
                                 (swap! run-calls inc)
                                 {:some :process})
         e/running? (fn [_] (throw ex ))
         log/log* (fn [_logger level _throwable message]
                                      (swap! warnings-logged conj [level message]))]
        (is (thrown-with-msg?
              ExceptionInfo
              #"gave up trying to launch :firefox after 8 tries"
              (e/with-firefox {:webdriver-failed-launch-retries 7} driver
                driver)))
        (is (= 8 @run-calls) "run calls")
        (is (= [[:warn "unexpected exception occurred launching :firefox, try 1 (of a max of 8)"]
                [:warn "unexpected exception occurred launching :firefox, try 2 (of a max of 8)"]
                [:warn "unexpected exception occurred launching :firefox, try 3 (of a max of 8)"]
                [:warn "unexpected exception occurred launching :firefox, try 4 (of a max of 8)"]
                [:warn "unexpected exception occurred launching :firefox, try 5 (of a max of 8)"]
                [:warn "unexpected exception occurred launching :firefox, try 6 (of a max of 8)"]
                [:warn "unexpected exception occurred launching :firefox, try 7 (of a max of 8)"]]
               @warnings-logged) "warnings logged"))))
  (testing "succeed before max tries"
    (let [run-calls (atom 0)
          succeed-when-calls 3
          warnings-logged (atom [])
          ex (ex-info "safari badness" {})]
      (with-redefs
        [etaoin.impl.proc/run  (fn [_ _]
                                 (swap! run-calls inc)
                                 {:some :process})
         e/create-session   (fn [_ _] "session-key")
         proc/kill identity
         e/delete-session   identity
         e/running? (fn [_]
                      (if (< @run-calls succeed-when-calls)
                        (throw ex)
                        true))
         log/log* (fn [_logger level _throwable message]
                                      (swap! warnings-logged conj [level message]))
         util/get-free-port (constantly 12345)]
        ;; safari driver has a default of 4 retries
        (e/with-safari driver
          (is (= {:args ["safaridriver" "--port" 12345]
                  :capabilities {:loggingPrefs {:browser "ALL"}}
                  :host "127.0.0.1"
                  :locator "xpath"
                  :port 12345
                  :process {:some :process}
                  :session "session-key"
                  :type :safari,
                  :url "http://127.0.0.1:12345"} driver)))
        (is (= succeed-when-calls @run-calls))
        (is (= [[:warn "unexpected exception occurred launching :safari, try 1 (of a max of 5)"]
                [:warn "unexpected exception occurred launching :safari, try 2 (of a max of 5)"]]
               @warnings-logged) "warnings logged")))))

(deftest test-actions
  (let [keyboard        (-> (e/make-key-input)
                            (e/with-key-down "H")
                            e/add-pause
                            (e/with-key-down "I")
                            (dissoc :id))
        mouse           (-> (e/make-mouse-input)
                            e/add-pointer-click
                            e/add-pause
                            (e/with-pointer-left-btn-down
                              (e/add-pointer-move-to-el "123"))
                            (dissoc :id))
        keyboard-result {:type "key",
                         :actions
                         [{:type "keyDown", :value "H"}
                          {:type "keyUp", :value "H"}
                          {:type "pause", :duration 0}
                          {:type "keyDown", :value "I"}
                          {:type "keyUp", :value "I"}]}
        mouse-result    {:type       "pointer",
                         :actions
                         [{:type "pointerDown", :duration 0, :button 0}
                          {:type "pointerUp", :duration 0, :button 0}
                          {:type "pause", :duration 0}
                          {:type "pointerDown", :duration 0, :button 0}
                          {:type     "pointerMove",
                           :x        0,
                           :y        0,
                           :origin   {:ELEMENT "123", :element-6066-11e4-a52e-4f735466cecf "123"},
                           :duration 250}
                          {:type "pointerUp", :duration 0, :button 0}],
                         :parameters {:pointerType :mouse}}]
    (is (= keyboard-result keyboard))
    (is (= mouse-result mouse))))

(deftest test-find-tests
  (let [parsed-file {:tests  [{:id 1} {:id 2} {:id 3} {:id 4}]
                     :suites [{:id 1 :name "Suite 1" :tests [1 2]}
                              {:id 2 :name "Suite 2" :tests [3 4]}]}]
    (is (= [{:id 1} {:id 2} {:id 4}]
           (ide/find-tests {:suite-id 1 :test-id 4} parsed-file)))
    (is (= [{:id 1} {:id 2} {:id 3} {:id 4}]
           (ide/find-tests {:suite-ids [1] :suite-name "Suite 2" :test-id 4} parsed-file)))
    (is (= [{:id 1} {:id 2} {:id 3} {:id 4}]
           (ide/find-tests {} parsed-file)))))

(def commands
  [
   {:command :times}
   {:command :do-times}
   {:command :end}
   {:command :while}
   {:command :do-while}
   {:command :end}
   {:command :do}
   {:command :do-do}
   {:command :repeatIf}
   {:command :do-1}
   {:command :if}
   {:command :do-2}
   {:command :if}
   {:command :do-AAA}
   {:command :end}
   {:command :end}
   {:command :do-3}
   {:command :forEach}
   {:command :if}
   {:command :do-if}
   {:command :elseIf}
   {:command :do-else-if1}
   {:command :elseIf}
   {:command :do-else-if2}
   {:command :else}
   {:command :do-else}
   {:command :end}
   {:command :end}
   {:command :do-3 :opensWindow true}
   ])

(def valid-commands-tree
  [[:times
    {:this   {:command :times},
     :branch [[:cmd {:command :do-times}]],
     :end    {:command :end}}]
   [:while
    {:this   {:command :while},
     :branch [[:cmd {:command :do-while}]],
     :end    {:command :end}}]
   [:do
    {:this      {:command :do},
     :branch    [[:cmd {:command :do-do}]],
     :repeat-if {:command :repeatIf}}]
   [:cmd {:command :do-1}]
   [:if
    {:if
     {:this {:command :if},
      :branch
      [[:cmd {:command :do-2}]
       [:if
        {:if  {:this {:command :if}, :branch [[:cmd {:command :do-AAA}]]},
         :end {:command :end}}]]},
     :end {:command :end}}]
   [:cmd {:command :do-3}]
   [:for-each
    {:this {:command :forEach},
     :branch
     [[:if
       {:if   {:this {:command :if}, :branch [[:cmd {:command :do-if}]]},
        :else-if
        [{:this {:command :elseIf}, :branch [[:cmd {:command :do-else-if1}]]}
         {:this {:command :elseIf}, :branch [[:cmd {:command :do-else-if2}]]}],
        :else {:this {:command :else}, :branch [[:cmd {:command :do-else}]]},
        :end  {:command :end}}]],
     :end  {:command :end}}]
   [:cmd-with-open-window {:command :do-3, :opensWindow true}]])

(deftest parse-commands-tree
  (let [invalid-commands [{:command :if}
                          {:command :if}
                          {:command :do-something}
                          {:command :end}]]
    (is (= (s/conform ::spec/commands commands)
           valid-commands-tree))
    (is (s/invalid? (s/conform ::spec/commands invalid-commands)))))
