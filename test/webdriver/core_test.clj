(ns webdriver.core-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+]]
            [clojure.test :refer [is
                                  deftest
                                  use-fixtures
                                  testing]]
            [webdriver.dsl :refer [with-server
                                   wait-running
                                   with-session
                                   go-url

                                   visible

                                   with-url
                                   with-title

                                   get-url
                                   get-title

                                   clear
                                   clear-form

                                   fill
                                   fill-form
                                   submit-form

                                   with-proc
                                   with-proc-multi
                                   random-port

                                   with-xpath
                                   click

                                   with-css
                                   with-csss

                                   wait
                                   text

                                   back
                                   forward
                                   refresh
                                   ]]))

(def host "127.0.0.1")
(def port 6666)

(defn fixture-browsers [f]

  ;; "-v"

  (with-proc p [["geckodriver" "--host" host "--port" port]]
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

(deftest test-local-file
  (let [url (-> "html/test.html" io/resource str)]

    (wait-running :message "The server did not start.")
    (with-session {} {}

      (go-url url)

      (testing "title"
        (let [title (get-title)]
          (is (= title "Webdriver Test Document")))
        (with-title title
          (is (= title "Webdriver Test Document"))))

      (testing "click"
        (let [t (text "//span[@id='baz']")]
          (is (= t "")))
        (with-xpath
          (click "//button[@id='foo']"))
        (let [t (text "//span[@id='baz']")]
          (is (= t "clicked"))))

      (testing "css test"
        (with-xpath
          (with-css "//div[@id='css-test']" width
            (is (= width "250px")))

          (with-csss "//div[@id='css-test']" [width
                                              height
                                              background-color]
            (is (= width "250px"))
            (is (= height "150px"))
            (is (or
                 ;; firefox
                 (= background-color "rgb(0, 0, 0)")
                 ;; chrome, phantom
                 (= background-color "rgba(0, 0, 0, 1)"))))))

      (testing "input"
        (testing "simple input"
          (with-xpath
            (fill "//input[@id='simple-input']" "test")
            (click "//input[@id='simple-submit']"))
          (with-url url
            (is (str/includes? url "login=test"))))

        (testing "form input"
          (with-xpath
            (fill-form "//form[@id='submit-test']" {:login "Ivan"
                                                    :password "lalilulelo"
                                                    :message "long_text_here"
                                                    })
            (click "//input[@id='simple-submit']"))
          (with-url url
            (is (str/includes? url "login=Ivan"))
            (is (str/includes? url "password=lalilulelo"))
            (is (str/includes? url "message=long_text_here"))))

        ;; form submit
        ;; any button type

        )


      ;; (let [url (get-url)]
      ;;   (is (= url "https://ya.ru/")))
      ;; (with-url url
      ;;   (is (= url "https://ya.ru/")))
      )))

(deftest test-clear
  (let [url (-> "html/test.html" io/resource str)
        form "//form[@id='submit-test']"
        input "//input[@id='simple-input']"
        submit "//input[@id='simple-submit']"]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
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

;; (deftest test-wait
;;   (let [url (-> "html/test.html" io/resource str)
;;         form "//form[@id='submit-test']"
;;         input "//input[@id='simple-input']"
;;         submit "//input[@id='simple-submit']"]
;;     (wait-running :message "The server did not start.")
;;     (with-session {} {}
;;       (go-url url)
;;       (testing "simple clear"
;; )
;; )))

(deftest test-visible
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
      (is (visible "//button[@id='button-visible']"))
      (is (not (visible "//button[@id='button-hidden']")))
      (is (not (visible "//div[@id='div-hidden']")))
      (try+
       (is (thrown? clojure.lang.ExceptionInfo
                    (visible "//test[@id='dunno-foo-bar']"))))
      ;; (is (not (visible "//div[@id='div-covered']")))
)))


(deftest test-url-title
  (with-server host port
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url "http://ya.ru")
      (let [url (get-url)]
        (is (= url "https://ya.ru/")))
      (with-url url
        (is (= url "https://ya.ru/")))
      (let [title (get-title)]
        (is (= title "Яндекс")))
      (with-title title
        (is (= title "Яндекс"))))))

(deftest test-navigation
  (with-server host port
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url "http://ya.ru")
      (go-url "http://mail.ru")
      (back)
      (refresh)
      (forward)
      (refresh)
      (let [url (get-url)]
        (is (= url "https://mail.ru/"))))))
