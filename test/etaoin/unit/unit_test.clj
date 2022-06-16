(ns etaoin.unit.unit-test
  (:require
   [babashka.fs :as fs]
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [etaoin.api :as e]
   [etaoin.api2 :as e2]
   [etaoin.ide.flow :as ide]
   [etaoin.ide.impl.spec :as spec]
   [etaoin.impl.proc :as proc]
   [etaoin.test-report]))

(deftest test-firefox-driver-args
  (with-redefs
   [etaoin.impl.proc/run  (fn [_ _])
    e/wait-running     identity
    e/create-session   (fn [_ _] "session-key")
    proc/kill identity
    e/delete-session   identity]
    (testing "Session"
      (e2/with-firefox [driver]
        (is (= "session-key"
               (:session driver)))))
    (testing "No custom args"
      (e2/with-firefox [driver {:port 1234}]
        (is (= ["geckodriver" "--port" 1234]
               (:args driver)))))
    (testing "Default `--marionette-port` is assigned when `:profile` is specified"
      (e2/with-firefox [driver {:port 1234 :profile "/tmp/firefox-profile/1"}]
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2828]
               (:args driver)))))
    (testing "Custom `--marionette-port` is assigned when `:profile` is specified"
      (e2/with-firefox [driver {:port        1234
                                :profile     "/tmp/firefox-profile/1"
                                :args-driver ["--marionette-port" 2821]}]
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2821]
               (:args driver)))))))

(deftest test-chrome-profile
  (fs/with-temp-dir [chrome-dir {:prefix "chrome-dir"}]
    (let [profile-path (str (fs/file chrome-dir "chrome-profile"))]
      (e2/with-chrome [driver {:profile profile-path :args ["--no-sandbox"]}]
        (e/go driver "chrome://version")
        (is profile-path
            (e/get-element-text driver :profile_path))))))

(deftest test-fail-run-driver
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"wrong-driver-path"
        (e/chrome {:path-driver "wrong-driver-path"}))))

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
