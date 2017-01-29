(ns webdriver.core-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+]]
            [clojure.test :refer [is
                                  deftest
                                  use-fixtures
                                  testing]]
            [webdriver.dsl :refer :all]))

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

(deftest test-enabled
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
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
      (go-url url)
      (with-xpath
        (is (exists "//html"))
        (is (exists "//body"))
        (is (not (exists "//test[@id='dunno-foo-bar']")))))))

(deftest test-alert
  ;; todo skip decorators
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
      (click "//button[@id='button-alert']")
      (is (= (get-alert-text) "Hello!"))
      (is (alert-open))
      (accept-alert)
      (is (not (alert-open)))
      (click "//button[@id='button-alert']")
      (is (alert-open))
      (dismiss-alert)
      (is (not (alert-open))))))

(deftest test-attributes
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
      (testing "common attributes"
        (with-xpath
          (with-attrs "//input[@id='input-attr']"
            [id type value name style
             disabled data-foo data-bar]
            (is (= id "input-attr"))
            (is (= type "text"))
            (is (= value "hello"))
            (is (= style "border: 5px; width: 150px;"))
            (is (= disabled "true"))
            (is (= data-foo "foo"))
            (is (= data-bar "bar")))))
      (testing "event attributes"
        (with-xpath
          (with-attrs "//input[@id='input-attr']" [onclick]
            (is (= onclick "alert(123)")))))
      (testing "missing attributes"
        (with-xpath
          (with-attrs "//input[@id='input-attr']" [foo bar baz dunno]
            (is (= foo nil))
            (is (= baz nil))
            (is (= bar nil))
            (is (= dunno nil))))))))

(deftest test-title
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (testing "empty page"
        (with-title title
          (is (= title ""))))
      (testing "go URL"
        (go-url url)
        (with-title title
          (is (= title "Webdriver Test Document")))))))

(deftest test-url
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (testing "empry page"
        (with-url url
          (is (or (= url "about:blank")
                  (= url "data:,")))))
      (testing "go URL"
        (go-url url)
        (with-url url
          (is (str/ends-with? url "/resources/html/test.html")))))))
