(ns etaoin.unit-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [etaoin.ide :as ide]
            etaoin.proc)
  (:import java.io.File
           java.nio.file.attribute.FileAttribute
           java.nio.file.Files
           org.apache.commons.io.FileUtils))

(defmacro with-tmp-dir [prefix bind & body]
  `(let [tmp#  (str (Files/createTempDirectory
                      ~prefix
                      (into-array FileAttribute [])))
         ~bind tmp#]
     (try
       ~@body
       (finally
         (FileUtils/deleteDirectory (File. tmp#))))))

(deftest test-firefox-driver-args
  (with-redefs
    [etaoin.proc/run  (fn [_ _])
     wait-running     identity
     create-session   (fn [_ _] "session-key")
     etaoin.proc/kill identity
     delete-session   identity]
    (testing "Session"
      (with-firefox {} driver
        (is (= "session-key"
               (:session driver)))))
    (testing "No custom args"
      (with-firefox {:port 1234} driver
        (is (= ["geckodriver" "--port" 1234]
               (:args driver)))))
    (testing "Default `--marionette-port` is assigned when `:profile` is specified"
      (with-firefox {:port 1234 :profile "/tmp/firefox-profile/1"} driver
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2828]
               (:args driver)))))
    (testing "Custom `--marionette-port` is assigned when `:profile` is specified"
      (with-firefox {:port        1234
                     :profile     "/tmp/firefox-profile/1"
                     :args-driver ["--marionette-port" 2821]} driver
        (is (= ["geckodriver" "--port" 1234 "--marionette-port" 2821]
               (:args driver)))))))

(deftest test-chrome-profile
  (with-tmp-dir "chrome-dir" chrome-dir
    (let [profile-path (str (File. chrome-dir "chrome-profile"))]
      (with-chrome {:profile profile-path :args ["--no-sandbox"]} driver
        (go driver "chrome://version")
        (is profile-path
            (get-element-text driver :profile_path))))))

(deftest test-fail-run-driver
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"wrong-driver-path"
        (chrome {:path-driver "wrong-driver-path"}))))


(deftest test-actions
  (let [keyboard        (-> (make-key-input)
                            (with-key-down "H")
                            add-pause
                            (with-key-down "I")
                            (dissoc :id))
        mouse           (-> (make-mouse-input)
                            add-pointer-click
                            add-pause
                            (with-pointer-left-btn-down
                              (add-pointer-move-to-el "123"))
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
                          {:type    "pointerMove",
                           :x       0,
                           :y       0,
                           :origin  {:ELEMENT "123", :element-6066-11e4-a52e-4f735466cecf "123"},
                           :duraion 250}
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
    (is (= (s/conform :etaoin.ide/commands commands)
           valid-commands-tree))
    (is (s/invalid? (s/conform :etaoin.ide/commands invalid-commands)))))
