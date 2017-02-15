(ns etaoin.api-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+]]
            [clojure.test :refer :all]
            [etaoin.api :refer :all])
  (:import javax.imageio.ImageIO))

;; (defmacro with-tmp-file [prefix suffix bind & body]
;;   `(let [tmp# (java.io.File/createTempFile ~prefix ~suffix)
;;          ~bind (.getAbsolutePath tmp#)]
;;      (try
;;        ~@body
;;        (finally
;;          (.delete tmp#)))))

(defn numeric? [val]
  (or (instance? Double val)
      (instance? Integer val)))

(def ^:dynamic *driver*)

(defn fixture-browsers [f]
  (let [url (-> "html/test.html" io/resource str)]
    (doseq [type [:firefox :chrome :phantom]]
      (with-driver type {} driver
        (go driver url)
        (wait-visible driver {:id :document-end})
        (binding [*driver* driver]
          (f))))))

(use-fixtures
  :each
  fixture-browsers)

(deftest test-visible
  (doto *driver*
    (-> (visible? {:id :button-visible}) is)
    (-> (invisible? {:id :button-hidden}) is)
    (-> (invisible? {:id :div-hidden}) is)
    (-> (invisible? {:id :dunno-foo-bar}) is)))

;; (deftest test-clear
;;   (let [form "//form[@id='submit-test']"
;;         input "//input[@id='simple-input']"
;;         submit "//input[@id='simple-submit']"]
;;     (testing "simple clear"
;;       (with-xpath
;;         (fill input "test")
;;         (clear input)
;;         (click submit)
;;         (let [url (get-url)]
;;           (is (str/ends-with? url "?login=&password=&message=")))))
;;     (testing "form clear"
;;       (with-xpath
;;         (fill-form form {:login "Ivan"
;;                          :password "lalilulelo"
;;                          :message "long_text_here"})
;;         (clear-form form)
;;         (click submit)
;;         (let [url (get-url)]
;;           (is (str/ends-with? url "?login=&password=&message=")))))))

(deftest test-enabled
  (doto *driver*
    (-> (disabled? {:id :input-disabled}) is)
    (-> (enabled? {:id :input-not-disabled}) is)
    (-> (disabled? {:id :textarea-disabled}) is))
  (is (thrown?
       clojure.lang.ExceptionInfo
       (enabled? *driver* {:id :dunno-foo-bar}))))

(deftest test-exists
  (doto *driver*
    (-> (exists? {:tag :html}) is)
    (-> (exists? {:tag :body}) is)
    (-> (absent? {:id :dunno-foo-bar}) is)))

(deftest test-alert
  (skip-phantom
   *driver*
   (doto *driver*
     (click {:id :button-alert})
     (-> get-alert-text (= "Hello!") is)
     (-> has-alert? is)
     (accept-alert)
     (-> has-alert? not is)
     (click {:id :button-alert})
     (-> has-alert? is)
     (dismiss-alert)
     (-> has-alert? not is))))

(deftest test-attributes
  (testing "common attributes"
    (doto *driver*
      (-> (get-element-attrs
           {:id :input-attr}
           :id :type :value :name :style
           "disabled" "data-foo" "data-bar")
          (= ["input-attr"
              "text"
              "hello"
              "test"
              "border: 5px; width: 150px;"
              "true"
              "foo"
              "bar"])
          is)))
  (testing "event attributes"
    (doto *driver*
      (-> (get-element-attr
           {:id :input-attr}
           :onclick)
          (= "alert(123)")
          is)))
  (testing "missing attributes"
    (doto *driver*
      (-> (get-element-attrs
           {:id :input-attr}
           :foo "bar" :baz "dunno")
          (= [nil nil nil nil])
          is))))

(deftest test-title
  (doto *driver*
    (-> get-title (= "Webdriver Test Document") is)))

(deftest test-url
  (doto *driver*
    (-> get-url
        (str/ends-with? "/resources/html/test.html")
        is)))

;; (deftest test-css-props
;;   (testing "single css"
;;     (with-css "//div[@id='div-css-simple']" display
;;       (is (= display "block"))))
;;   (testing "multiple css"
;;     (with-csss "//div[@id='div-css-simple']"
;;       [display background-color width height]
;;       (is (= display "block"))
;;       (is (or (= background-color "rgb(204, 204, 204)")
;;               (= background-color "rgba(204, 204, 204, 1)")))
;;       (is (= width "150px"))
;;       (is (= height "250px"))))
;;   (testing "styled css"
;;     (with-csss "//div[@id='div-css-styled']"
;;       [display width height]
;;       (is (= display "block"))
;;       (is (= width "333px"))
;;       (is (= height "111px"))))
;;   (testing "missing css"
;;     (with-csss "//div[@id='div-css-styled']"
;;       [foo bar baz]
;;       (is (nil? foo))
;;       (is (nil? bar))
;;       (is (nil? baz)))))

;; (deftest test-wait-text
;;   (testing "wait for text simple"
;;     (refresh)
;;     (with-xpath
;;       (click "//button[@id='wait-button']"))
;;     (wait-has-text "-secret-")
;;     (is true "text found"))
;;   (testing "wait for text timeout"
;;     (refresh)
;;     (with-xpath
;;       (click "//button[@id='wait-button']"))
;;     (try+
;;      (wait-has-text "-secret-" :timeout 1
;;                     :message "No -secret- text on the page.")
;;      (is false "should not be executed")
;;      (catch [:type :etaoin/timeout] data
;;        (is (= (-> data (dissoc :predicate))
;;               {:type :etaoin/timeout
;;                :message "No -secret- text on the page."
;;                :timeout 1
;;                :poll 0.5
;;                :times 3}))
;;        (is true "exception was caught"))))
;;   (testing "wait non-existing text"
;;     (refresh)
;;     (try+
;;      (wait-has-text "whatever-foo-bar-")
;;      (is false "should not be executed")
;;      (catch [:type :etaoin/timeout] data
;;        (is (= (-> data (dissoc :predicate))
;;               {:type :etaoin/timeout
;;                :message nil
;;                :timeout 10
;;                :poll 0.5
;;                :times 21}))
;;        (is true "exception was caught")))))

;; (deftest test-wait-has-class
;;   (testing "wait for has class"
;;     (refresh)
;;     (click "//*[@id='wait-add-class-trigger']")
;;     (wait-for-has-class
;;      "//*[@id='wait-add-class-target']"
;;      "new-one")
;;     (is true)))

;; (deftest test-close-window
;;   (skip-firefox
;;    (close)
;;    (try+
;;     (let [url (get-url)]
;;       (is false))
;;     (catch [:type :etaoin/http-error] _
;;       (is true)))))

;; (deftest test-drag-and-drop
;;   (let [url "http://marcojakob.github.io/dart-dnd/basic/web/"
;;         doc "//*[@class='document']"
;;         trash "//div[contains(@class, 'trash')]"]
;;     (skip-firefox
;;      (testing "moving elements"
;;        (go url)
;;        (wait 1)
;;        (drag-and-drop doc trash)
;;        (wait 1)
;;        (drag-and-drop doc trash)
;;        (wait 1)
;;        (drag-and-drop doc trash)
;;        (wait 1)
;;        (drag-and-drop doc trash)
;;        (is true)))))

(deftest test-element-location
  (let [q {:id :el-location-input}
        loc (get-element-location *driver* q)
        {:keys [x y]} loc]
    (is (numeric? x))
    (is (numeric? y))))

;; (deftest test-window-position
;;   (testing "getting position"
;;     (let-window-position {:keys [x y]}
;;       (is (numeric? x))
;;       (is (numeric? y))))
;;   (testing "setting position"
;;     (skip-phantom
;;      (set-window-position 500 500)
;;      (with-window-position {:x 222 :y 333}
;;        (let-window-position {:keys [x y]}
;;          (is (numeric? x))
;;          (is (numeric? y)))))))

(deftest test-window-size
  (testing "getting size"
    (let [{:keys [width height]} (get-window-size *driver*)]
      (is (numeric? width))
      (is (numeric? height))
      (set-window-size *driver* (+ width 10) (+ height 10))
      (let [{:keys [width' height']} (get-window-size *driver*)]
        (not= width width')
        (not= height height')))))

;; (deftest test-active-element
;;   (testing "active element"
;;     (click "//*[@id='set-active-el']")
;;     (let-active-el el
;;       (with-attr-el el id
;;         (is (= id "active-el-input"))))))

;; (deftest test-element-text
;;   (with-text "//*[@id='element-text']" text
;;     (is (= text "Element text goes here."))))

;; (deftest test-element-value
;;   (when-chrome
;;       (with-value "//*[@id='element-value']" value
;;         (is (= value "value text")))))

;; (deftest test-cookies
;;   (testing "getting all cookies"
;;     (with-cookies cookies
;;       (when-chrome
;;           (is (= cookies [])))
;;       (when-firefox
;;           (is (= cookies [{:name "cookie1",
;;                            :value "test1",
;;                            :path "/",
;;                            :domain "",
;;                            :expiry nil,
;;                            :secure false,
;;                            :httpOnly false}
;;                           {:name "cookie2",
;;                            :value "test2",
;;                            :path "/",
;;                            :domain "",
;;                            :expiry nil,
;;                            :secure false,
;;                            :httpOnly false}])))
;;       (when-phantom
;;           (is (= cookies [{:domain "",
;;                            :httponly false,
;;                            :name "cookie2",
;;                            :path "/",
;;                            :secure false,
;;                            :value "test2"}
;;                           {:domain "",
;;                            :httponly false,
;;                            :name "cookie1",
;;                            :path "/",
;;                            :secure false,
;;                            :value "test1"}])))))
;;   (testing "getting named cookie"
;;     (with-named-cookies "cookie2" cookies
;;       (when-chrome
;;           (is (= cookies [])))
;;       (when-firefox
;;           (is (= cookies [{:name "cookie2"
;;                            :value "test2"
;;                            :path "/"
;;                            :domain ""
;;                            :expiry nil
;;                            :secure false
;;                            :httpOnly false}])))
;;       (when-phantom
;;           (is (= cookies
;;                  [{:domain ""
;;                    :httponly false
;;                    :name "cookie2"
;;                    :path "/"
;;                    :secure false
;;                    :value "test2"}])))))
;;   (testing "setting a cookie"
;;     (skip-phantom
;;      (set-cookie {:httponly false
;;                   :name "cookie3"
;;                   :domain ""
;;                   :secure false
;;                   :value "test3"})
;;      (with-named-cookies "cookie3" cookies
;;        (when-firefox
;;            (is (= cookies
;;                   [{:name "cookie3"
;;                     :value "test3"
;;                     :path ""
;;                     :domain ""
;;                     :expiry nil
;;                     :secure false
;;                     :httpOnly false}]))))))

;;   (testing "deleting a named cookie"
;;     (skip-phantom
;;      (set-cookie {:httponly false
;;                   :name "cookie3"
;;                   :domain ""
;;                   :secure false
;;                   :value "test3"})
;;      (with-named-cookies "cookie3" cookies
;;        (when-firefox
;;            (is (= cookies
;;                   [{:name "cookie3"
;;                     :value "test3"
;;                     :path ""
;;                     :domain ""
;;                     :expiry nil
;;                     :secure false
;;                     :httpOnly false}]))))))
;;   (testing "deleting a named cookie"
;;     (delete-cookie "cookie3")
;;     (with-named-cookies "cookie3" cookies
;;       (is (= cookies []))))
;;   (testing "deleting all cookies"
;;     (delete-cookies)
;;     (with-cookies cookies
;;       (is (= cookies [])))))

;; (deftest test-page-source
;;   (let-source src
;;     (when-firefox
;;         (is (str/starts-with? src "<html><head>")))
;;     (skip-firefox
;;         (is (str/starts-with? src "<!DOCTYPE html>")))))

;; (deftest test-element-properties
;;   (when-firefox
;;       (let-prop "//*[@id='element-props']" innerHTML
;;                 (is (= innerHTML "<div>Inner HTML</div>")))
;;     (let-props "//*[@id='element-props']" [innerHTML tagName]
;;                (is (= innerHTML "<div>Inner HTML</div>"))
;;                (is (= tagName "DIV")))))

;; (deftest test-screenshot
;;   (with-tmp-file "screenshot" ".png" path
;;     (screenshot path)
;;     (-> path
;;         io/file
;;         ImageIO/read
;;         is)))

;; (deftest test-js-execute
;;   (testing "simple result"
;;     (let [result (js-execute "return 42;")]
;;       (is (= result 42))))
;;   (testing "with args"
;;     (let [script "return {foo: arguments[0], bar: arguments[1]};"
;;           result (js-execute script {:test 42} [true, nil, "Hello"])]
;;       (is (= result
;;              {:foo {:test 42}
;;               :bar [true nil "Hello"]})))))

;; (deftest test-js-inject
;;   (let [url (-> "html/test.html" io/resource str)
;;         js-url (-> "js/inject.js" io/resource str)]
;;     (testing "adding a script"
;;       (add-script js-url)
;;       (let [result (js-execute "return injected_func();")]
;;         (is (= result "I was injected"))))))

;; (deftest test-set-hash
;;   (testing "set hash"
;;     (set-hash "hello")
;;     (let [url (get-url)]
;;       (is (str/ends-with? url "/test.html#hello")))
;;     (set-hash "goodbye")
;;     (let [url (get-url)]
;;       (is (str/ends-with? url "/test.html#goodbye")))))
