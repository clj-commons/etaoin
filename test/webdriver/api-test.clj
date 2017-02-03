(ns webdriver.api-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+]]
            [clojure.test :refer [is
                                  deftest
                                  use-fixtures
                                  testing]]
            [webdriver.test :refer :all]))

(def host "127.0.0.1")
(def port 6666)

(defn fixture-browsers [f]

  ;; "-v"

  (with-proc p [["geckodriver" "--host" host "--port" port "--log" "fatal"]]
    (testing "firefox"
      (with-server {:host host :port port :browser :firefox}
        (f))))

  ;; "--log-path=/Users/ivan/webdriver666.txt"
  ;; "--verbose"

  (with-proc p [["chromedriver"  (str "--port=" port) ]]
    (testing "chrome"
      (with-server {:host host :port port :browser :chrome}
        (f))))

  (with-proc p [["phantomjs" "--webdriver" port]]
    (testing "phantom"
      (with-server {:host host :port port :browser :phantom}
        (f))))

  )

(use-fixtures
  :each
  fixture-browsers)

(deftest test-clear
  (let [url (-> "html/test.html" io/resource str)
        form "//form[@id='submit-test']"
        input "//input[@id='simple-input']"
        submit "//input[@id='simple-submit']"]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go url)
      (testing "simple clear"
        (with-xpath
          (fill input "test")
          (clear input)
          (click submit)
          (with-url url
            (is (str/ends-with? url "?login=&password=&message=")))))
      (testing "form clear"
        (with-xpath
          (fill-form form {:login "Ivan"
                           :password "lalilulelo"
                           :message "long_text_here"})
          (clear-form form)
          (click submit)
          (with-url url
            (is (str/ends-with? url "?login=&password=&message="))))))))

(deftest test-visible
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go url)
      (is (visible "//button[@id='button-visible']"))
      (is (not (visible "//button[@id='button-hidden']")))
      (is (not (visible "//div[@id='div-hidden']")))
      (try+
       (is (thrown? clojure.lang.ExceptionInfo
                    (visible "//test[@id='dunno-foo-bar']"))))
      ;; (is (not (visible "//div[@id='div-covered']")))
)))

(deftest test-enabled
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go url)
      (is (disabled "//input[@id='input-disabled']"))
      (is (enabled "//input[@id='input-not-disabled']"))
      (is (disabled "//textarea[@id='textarea-disabled']"))
      (try+
       (is (thrown? clojure.lang.ExceptionInfo
                    (enabled "//test[@id='dunno-foo-bar']")))))))

(deftest test-exists
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go url)
      (with-xpath
        (is (exists "//html"))
        (is (exists "//body"))
        (is (not (exists "//test[@id='dunno-foo-bar']")))))))

(deftest test-alert
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go url)
      (click "//button[@id='button-alert']")
      (skip-phantom
       (with-alert-text alert
         (is (= alert "Hello!")))
       (is (alert-open))
       (accept-alert)
       (is (not (alert-open)))
       (click "//button[@id='button-alert']")
       (is (alert-open))
       (dismiss-alert)
       (is (not (alert-open)))))))
