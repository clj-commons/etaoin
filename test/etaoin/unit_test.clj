(ns etaoin.unit-test
  (:require [etaoin.api :refer :all]
            [etaoin.proc]
            [clojure.test :refer :all])
  (:import (java.nio.file Files)
           (java.io File)
           (org.apache.commons.io FileUtils)
           (java.nio.file.attribute FileAttribute)))

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
