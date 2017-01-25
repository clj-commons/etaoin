(ns webdriver.core-test
  (:require [clojure.test :refer [is deftest use-fixtures testing]]
            [webdriver.dsl :refer [with-server
                                   wait-running
                                   with-session
                                   go-url

                                   with-url
                                   with-title

                                   get-url
                                   get-title


                                   with-proc
                                   with-proc-multi
                                   random-port

                                   wait

                                   back
                                   forward
                                   refresh
                                   ]]))

(def host "127.0.0.1")
(def port 6666)

(defn fixture-browsers [f]

  (with-proc p [["geckodriver" "-v" "--host" host "--port" port]]
    (testing "firefox"
      (f)))

  (with-proc p [["chromedriver" "--verbose" (str "--port=" port)]]
    (testing "chrome"
      (f)))

  (with-proc p [["phantomjs" "--webdriver" port]]
    (testing "phantom"
      (f))))

(use-fixtures
  :each
  fixture-browsers)

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
