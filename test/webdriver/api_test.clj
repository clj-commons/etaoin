(ns webdriver.api-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+]]
            [clojure.test :refer :all]
            [webdriver.api :refer :all]
            [webdriver.client :refer [with-pool]])
  (:import javax.imageio.ImageIO))

(defmacro with-tmp-file [prefix suffix bind & body]
  `(let [tmp# (java.io.File/createTempFile ~prefix ~suffix)
         ~bind (.getAbsolutePath tmp#)]
     (try
       ~@body
       (finally
         (.delete tmp#)))))

(defn numeric? [val]
  (or (instance? Double val)
      (instance? Integer val)))

(def host "127.0.0.1")
(def port 6666)

(defmacro with-driver-boot [driver & body]
  `(with-pool nil
     (with-browser ~driver
       (with-webdriver port
         (with-connect host port
           (with-session
             (go (-> "html/test.html" io/resource str))
             ~@body))))))

(defn fixture-browsers [f]
  (with-driver-boot :firefox (f))
  (with-driver-boot :chrome (f))
  (with-driver-boot :phantom (f)))

(use-fixtures
  :each
  fixture-browsers)

(deftest test-clear
  (let [form "//form[@id='submit-test']"
        input "//input[@id='simple-input']"
        submit "//input[@id='simple-submit']"]
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
          (is (str/ends-with? url "?login=&password=&message=")))))))

(deftest test-visible
  (is (visible "//button[@id='button-visible']"))
  (is (not (visible "//button[@id='button-hidden']")))
  (is (not (visible "//div[@id='div-hidden']")))
  (is (thrown? clojure.lang.ExceptionInfo
               (visible "//test[@id='dunno-foo-bar']"))))

(deftest test-enabled
  (is (disabled "//input[@id='input-disabled']"))
  (is (enabled "//input[@id='input-not-disabled']"))
  (is (disabled "//textarea[@id='textarea-disabled']"))
  (is (thrown? clojure.lang.ExceptionInfo
               (enabled "//test[@id='dunno-foo-bar']"))))

(deftest test-exists
  (with-xpath
    (is (exists "//html"))
    (is (exists "//body"))
    (is (not (exists "//test[@id='dunno-foo-bar']")))))

(deftest test-alert
  (click "//button[@id='button-alert']")
  (skip-phantom
   (with-alert-text alert
     (is (= alert "Hello!")))
   (is (has-alert))
   (accept-alert)
   (is (not (has-alert)))
   (click "//button[@id='button-alert']")
   (is (has-alert))
   (dismiss-alert)
   (is (not (has-alert)))))

(deftest test-attributes
  (testing "common attributes"
    (with-attrs "//input[@id='input-attr']"
      [id type value name style
       disabled data-foo data-bar]
      (is (= id "input-attr"))
      (is (= type "text"))
      (is (= value "hello"))
      (is (= style "border: 5px; width: 150px;"))
      (is (= disabled "true"))
      (is (= data-foo "foo"))
      (is (= data-bar "bar"))))
  (testing "event attributes"
    (with-attrs "//input[@id='input-attr']" [onclick]
      (is (= onclick "alert(123)"))))
  (testing "missing attributes"
    (with-attrs "//input[@id='input-attr']"
      [foo bar baz dunno]
      (is (= foo nil))
      (is (= baz nil))
      (is (= bar nil))
      (is (= dunno nil)))))

(deftest test-title
  (with-title title
    (is (= title "Webdriver Test Document"))))

(deftest test-url
  (with-url url
    (is (str/ends-with? url "/resources/html/test.html"))))

(deftest test-css-props
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
      (is (nil? baz)))))

(deftest test-wait-text
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
       (is true "exception was caught")))))

(deftest test-wait-has-class
  (testing "wait for has class"
    (refresh)
    (click "//*[@id='wait-add-class-trigger']")
    (wait-for-has-class
     "//*[@id='wait-add-class-target']"
     "new-one")
    (is true)))

(deftest test-close-window
  (skip-firefox
   (close)
   (try+
    (with-url url
      (is false))
    (catch [:type :webdriver/http-error] _
      (is true)))))

(deftest test-drag-and-drop
  (let [url "http://marcojakob.github.io/dart-dnd/basic/web/"
        doc "//*[@class='document']"
        trash "//div[contains(@class, 'trash')]"]
    (skip-firefox
     (testing "wait for has class"
       (go url)
       (wait 1)
       (drag-and-drop doc trash)
       (wait 1)
       (drag-and-drop doc trash)
       (wait 1)
       (drag-and-drop doc trash)
       (wait 1)
       (drag-and-drop doc trash)
       (is true)))))

(deftest test-element-location
  (with-el-location "//*[@id='el-location-input']"
    {:keys [x y]}
    (is (numeric? x))
    (is (numeric? y))))

(deftest test-window-position
  (testing "getting position"
    (let-window-position {:keys [x y]}
      (is (numeric? x))
      (is (numeric? y))))
  (testing "setting position"
    (skip-phantom
     (set-window-position 500 500)
     (with-window-position {:x 222 :y 333}
       (let-window-position {:keys [x y]}
         (is (numeric? x))
         (is (numeric? y)))))))

(deftest test-window-size
  (testing "getting size"
    (let-window-size {:keys [width height]}
      (is (numeric? width))
      (is (numeric? height))))
  (testing "setting size"
    (with-window-size 555 666
      (let-window-size {:keys [width height]}
        (is (numeric? width))
        (is (numeric? height))))))

(deftest test-active-element
  (testing "active element"
    (click "//*[@id='set-active-el']")
    (let-active-el el
      (with-attr-el el id
        (is (= id "active-el-input"))))))

(deftest test-element-text
  (with-text "//*[@id='element-text']" text
    (is (= text "Element text goes here."))))

(deftest test-element-value
  (when-chrome
      (with-value "//*[@id='element-value']" value
        (is (= value "value text")))))

(deftest test-cookies
  (testing "getting all cookies"
    (with-cookies cookies
      (when-chrome
          (is (= cookies [])))
      (when-firefox
          (is (= cookies [{:name "cookie1",
                           :value "test1",
                           :path "/",
                           :domain "",
                           :expiry nil,
                           :secure false,
                           :httpOnly false}
                          {:name "cookie2",
                           :value "test2",
                           :path "/",
                           :domain "",
                           :expiry nil,
                           :secure false,
                           :httpOnly false}])))
      (when-phantom
          (is (= cookies [{:domain "",
                           :httponly false,
                           :name "cookie2",
                           :path "/",
                           :secure false,
                           :value "test2"}
                          {:domain "",
                           :httponly false,
                           :name "cookie1",
                           :path "/",
                           :secure false,
                           :value "test1"}])))))
  (testing "getting named cookie"
    (with-named-cookies "cookie2" cookies
      (when-chrome
          (is (= cookies [])))
      (when-firefox
          (is (= cookies [{:name "cookie2"
                           :value "test2"
                           :path "/"
                           :domain ""
                           :expiry nil
                           :secure false
                           :httpOnly false}])))
      (when-phantom
          (is (= cookies
                 [{:domain ""
                   :httponly false
                   :name "cookie2"
                   :path "/"
                   :secure false
                   :value "test2"}])))))
  (testing "setting a cookie"
    (skip-phantom
     (set-cookie {:httponly false
                  :name "cookie3"
                  :domain ""
                  :secure false
                  :value "test3"})
     (with-named-cookies "cookie3" cookies
       (when-firefox
           (is (= cookies
                  [{:name "cookie3"
                    :value "test3"
                    :path ""
                    :domain ""
                    :expiry nil
                    :secure false
                    :httpOnly false}]))))))

  (testing "deleting a named cookie"
    (skip-phantom
     (set-cookie {:httponly false
                  :name "cookie3"
                  :domain ""
                  :secure false
                  :value "test3"})
     (with-named-cookies "cookie3" cookies
       (when-firefox
           (is (= cookies
                  [{:name "cookie3"
                    :value "test3"
                    :path ""
                    :domain ""
                    :expiry nil
                    :secure false
                    :httpOnly false}]))))))
  (testing "deleting a named cookie"
    (delete-cookie "cookie3")
    (with-named-cookies "cookie3" cookies
      (is (= cookies []))))
  (testing "deleting all cookies"
    (delete-cookies)
    (with-cookies cookies
      (is (= cookies [])))))

(deftest test-page-source
  (let-source src
    (when-firefox
        (is (str/starts-with? src "<html><head>")))
    (when-not-firefox
        (is (str/starts-with? src "<!DOCTYPE html>")))))

(deftest test-element-properties
  (when-firefox
      (let-prop "//*[@id='element-props']" innerHTML
                (is (= innerHTML "<div>Inner HTML</div>")))
    (let-props "//*[@id='element-props']" [innerHTML tagName]
               (is (= innerHTML "<div>Inner HTML</div>"))
               (is (= tagName "DIV")))))

(deftest test-screenshot
  (with-tmp-file "screenshot" ".png" path
    (screenshot path)
    (-> path
        io/file
        ImageIO/read
        is)))

(deftest test-js-execute
  (testing "simple result"
    (let [result (js-execute "return 42;")]
      (is (= result 42))))
  (testing "with args"
    (let [script "return {foo: arguments[0], bar: arguments[1]};"
          result (js-execute script {:test 42} [true, nil, "Hello"])]
      (is (= result
             {:foo {:test 42}
              :bar [true nil "Hello"]})))))

(deftest test-js-inject
  (let [url (-> "html/test.html" io/resource str)
        js-url (-> "js/inject.js" io/resource str)]
    (testing "adding a script"
      (js-add-script js-url)
      (let [result (js-execute "return injected_func();")]
        (is (= result "I was injected"))))))

(deftest test-set-hash
  (testing "set hash"
    (js-set-hash "hello")
    (with-url url
      (is (str/ends-with? url "/test.html#hello")))
    (js-set-hash "goodbye")
    (with-url url
      (is (str/ends-with? url "/test.html#goodbye")))))
