(ns webdriver.core-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.test :refer [is
                                  deftest
                                  use-fixtures
                                  testing]]
            [webdriver.dsl :refer [with-server
                                   wait-running
                                   with-session
                                   go-url

                                   with-url
                                   with-title

                                   get-url
                                   get-title

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

  (with-proc p [["geckodriver" "-v" "--host" host "--port" port]]
    (testing "firefox"
      (with-server {:host host :port port :browser :firefox}
        (f))))

  ;; (with-proc p [["chromedriver" "--verbose" (str "--port=" port)]]
  ;;   (testing "chrome"
  ;;     (with-server {:host host :port port :browser :chrome}
  ;;       (f))))

  (with-proc p [["phantomjs" "--webdriver" port]]
    (testing "phantom"
      (with-server {:host host :port port :browser :phantom}
        (f)))))

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
                                                    :message "long_text_here"})
            (click "//input[@id='simple-submit']"))
          (with-url url
            (is (str/includes? url "login=Ivan"))
            (is (str/includes? url "password=lalilulelo"))
            (is (str/includes? url "message=long_text_here"))))

        ;; (testing "form submit"
        ;;   (with-xpath
        ;;     (submit-form "//form[@id='submit-test']" {:login "Ivan"
        ;;                                             :password "lalilulelo"
        ;;                                             :message "long_text_here"}))
        ;;   (with-url url
        ;;     (is (str/includes? url "login=Ivan"))
        ;;     (is (str/includes? url "password=lalilulelo"))
        ;;     (is (str/includes? url "message=long_text_here"))))






        ;; (with-xpath
        ;;   (submit-form "//div[@id='submit-test']" {:login "Ivan"
        ;;                                            :password "lalilulelo"
        ;;                                            }) ;; todo textarea
        ;;   (with-url url
        ;;     (is (= url "test")))


        ;;   )


        )

      ;; (let [url (get-url)]
      ;;   (is (= url "https://ya.ru/")))
      ;; (with-url url
      ;;   (is (= url "https://ya.ru/")))
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
