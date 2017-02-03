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

  ;; (with-proc p [["chromedriver"  (str "--port=" port) ]]
  ;;   (testing "chrome"
  ;;     (with-server {:host host :port port :browser :chrome}
  ;;       (f))))

  ;; (with-proc p [["phantomjs" "--webdriver" port]]
  ;;   (testing "phantom"
  ;;     (with-server {:host host :port port :browser :phantom}
  ;;       (f))))

  )

(use-fixtures
  :each
  fixture-browsers)

(deftest test-clear
  (let [url (-> "html/test.html" io/resource str)
        form "//form[@id='submit-test']"
        input "//input[@id='simple-input']"
        submit "//input[@id='simple-submit']"]
    ;; (wait-running :message "The server did not start.")
    (wait 1)
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
