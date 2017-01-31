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

  ;; (with-proc p [["geckodriver" "--host" host "--port" port "--log" "fatal"]]
  ;;   (testing "firefox"
  ;;     (with-server {:host host :port port :browser :firefox}
  ;;       (f))))

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
      (testing "empty page"
        (with-url url
          (is (or (= url "about:blank")
                  (= url "data:,")))))
      (testing "go URL"
        (go-url url)
        (with-url url
          (is (str/ends-with? url "/resources/html/test.html")))))))

(deftest test-css-props
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
      (testing "single css"
        (with-css "//div[@id='div-css-simple']" display
          (is (= display "block"))))
      (testing "multiple css"
        (with-csss "//div[@id='div-css-simple']"
          [display background-color width height]
          (is (= display "block"))
          (is (or (= background-color "rgb(204, 204, 204)")
                  (= background-color "rgba(204, 204, 204, 1)")))
          (is (= width "150px"))
          (is (= height "250px"))))
      (testing "styled css"
        (with-csss "//div[@id='div-css-styled']"
          [display width height]
          (is (= display "block"))
          (is (= width "333px"))
          (is (= height "111px"))))
      (testing "missing css"
        (with-csss "//div[@id='div-css-styled']"
          [foo bar baz]
          (is (nil? foo))
          (is (nil? bar))
          (is (nil? baz)))))))

(deftest test-wait-text
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)

      (testing "wait for text simple"
        (refresh)
        (with-xpath
          (click "//button[@id='wait-button']"))
        (wait-has-text "-secret-")
        (is true "text found"))

      (testing "wait for text timeout"
        (refresh)
        (with-xpath
          (click "//button[@id='wait-button']"))
        (try+
         (wait-has-text "-secret-" :timeout 1 :message "No -secret- text on the page.")
         (is false "should not be executed")
         (catch [:type :webdriver/timeout] data
           (is (= (-> data (dissoc :predicate))
                  {:type :webdriver/timeout
                   :message "No -secret- text on the page."
                   :timeout 1
                   :poll 0.5
                   :times 3}))
           (is true "exception was caught"))))

      (testing "wait non-existing text"
        (refresh)
        (try+
         (wait-has-text "whatever-foo-bar-")
         (is false "should not be executed")
         (catch [:type :webdriver/timeout] data
           (is (= (-> data (dissoc :predicate))
                  {:type :webdriver/timeout
                   :message nil
                   :timeout 10
                   :poll 0.5
                   :times 21}))
           (is true "exception was caught")))))))

(deftest test-wait-has-class
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)

      (testing "wait for has class"
        (refresh)
        (with-xpath
          (click "//*[@id='wait-add-class-trigger']"))
        (wait-for-has-class "//*[@id='wait-add-class-target']" "new-one")
        (is true "has class")))))

(deftest test-close-window
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
      (close)
      (try+
       (with-url url
         (is false))
        (catch [:type :webdriver/http-error] _
          (is true))))))

(deftest test-drag-and-drop
  (let [url "http://marcojakob.github.io/dart-dnd/basic/web/"
        doc "//*[@class='document']"
        trash "//div[contains(@class, 'trash')]"]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)
      (testing "wait for has class"
        (with-xpath
          (drag-and-drop doc trash)
          (wait 1)
          (drag-and-drop doc trash)
          (wait 1)
          (drag-and-drop doc trash)
          (wait 1)
          (drag-and-drop doc trash)
          (wait 1))
        (is true)))))

;; (deftest test-get-cookies
;;   (let [url (-> "html/test.html" io/resource str)]
;;     (wait-running :message "The server did not start.")
;;     (with-session {} {}
;;       (go-url url)
;;       (testing "get all cookies"
;;         (with-cookies cookies
;;           (is (= (->> cookies (sort-by :name) vec)
;;                  [{:domain "",
;;     :httponly false,
;;     :name "cookie1",
;;     :path "/",
;;     :secure false,
;;     :value "test1"}
;;    {:domain "",
;;     :httponly false,
;;     :name "cookie2",
;;     :path "/",
;;     :secure false,
;;     :value "test2"}]


;; ))))
;;       ;; (testing "get named cookie"
;;       ;;   (with-cookie "cookie1" cookie
;;       ;;     (is (= cookie
;;       ;;            {:domain ""
;;       ;;             :httponly false
;;       ;;             :name "cookie1"
;;       ;;             :path "/"
;;       ;;             :secure false
;;       ;;             :value "test1"}))))
;;       )))

(deftest test-foo
  (let [url (-> "html/test.html" io/resource str)]
    (wait-running :message "The server did not start.")
    (with-session {} {}
      (go-url url)

      ;; (is (= (get-window-position) 1))

      ;;(set-window-position 100 100)
      ;; (wait 4)
      (is 1)


      ;; (set-window-size 100 200)
      ;; (is 1)
      ;; (wait 4)

      ;; (is (= (w-handler) 1))
      ;; (is (= (w-size) 1))


      ;; (with-box "//*[@id='wait-add-class-trigger']" {:keys [x y]}
      ;;   (is (= x 1)))

      ;; (is (= (el-box "//*[@id='wait-add-class-trigger']") 1))

      ;; (is (= (el-size "//*[@id='wait-add-class-trigger']") 1))
      ;; (is (= (location "//*[@id='wait-add-class-trigger']") 1))

      )))
