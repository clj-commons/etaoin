(ns etaoin.api-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+]]
            [clojure.test :refer :all]
            [etaoin.api :refer :all])
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

(def ^:dynamic *driver*)

(defn fixture-browsers [f]
  (let [url (-> "html/test.html" io/resource str)]
    (doseq [type [:firefox :chrome :phantom :safari :headless]]
      (with-driver type {} driver
        (go driver url)
        (wait-visible driver {:id :document-end})
        (binding [*driver* driver]
          (testing (name type)
            (f)))))))

(use-fixtures
  :each
  fixture-browsers)

(deftest test-visible
  (doto *driver*
    (-> (visible? {:id :button-visible}) is)
    (-> (invisible? {:id :button-hidden}) is)
    (-> (invisible? {:id :div-hidden}) is)
    (-> (invisible? {:id :dunno-foo-bar}) is)))

(deftest test-input
  (testing "fill multiple imputs"
    (doto *driver*
      (fill-multi {:simple-input 1
                   :simple-password 2
                   :simple-textarea 3})
      (click :simple-submit)
      (when-safari (wait 3))
      (-> get-url
          (str/ends-with? "?login=1&password=2&message=3")
          is)))
  (testing "fill multiple vars"
    (doto *driver*
      (fill :simple-input 1 "test" 2 \space \A)
      (click :simple-submit)
      (when-safari (wait 3))
      (-> get-url
          (str/ends-with? "?login=1test2+A&password=&message=")
          is))))

(deftest test-clear
  (testing "simple clear"
    (doto *driver*
      (fill {:id :simple-input} "test")
      (clear {:id :simple-input})
      (click {:id :simple-submit})
      (when-safari (wait 3))
      (-> get-url
          (str/ends-with? "?login=&password=&message=")
          is)))

  (testing "multiple clear"
    (doto *driver*
      (fill-multi {:simple-input 1
                   :simple-password 2
                   :simple-textarea 3})
      (clear :simple-input
             :simple-password
             :simple-textarea)
      (when-safari (wait 3))
      (-> get-url
          (str/ends-with? "?login=&password=&message=")
          is))))

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

;; In Safari, alerts work quite slow, so we add some delays.
(deftest test-alert
  (when-not-phantom
    *driver*
    (doto *driver*
      (click {:id :button-alert})
      (when-safari (wait 1))
      (-> get-alert-text (= "Hello!") is)
      (-> has-alert? is)
      (accept-alert)
      (when-safari (wait 1))
      (-> has-alert? not is)
      (click {:id :button-alert})
      (when-safari (wait 1))
      (-> has-alert? is)
      (dismiss-alert)
      (when-safari (wait 1))
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
    (let [safari-val "function onclick(event) {\nalert(123)\n}"
          val (get-element-attr *driver*
                                {:id :input-attr}
                                :onclick)]
      (if (safari? *driver*)
        (is (= val safari-val))
        (is (= val "alert(123)")))))
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

(deftest test-css-props
  (testing "single css"
    (doto *driver*
      (-> (get-element-css {:id :div-css-simple} :display)
          (= "block")
          is)))
  (testing "multiple css"
    (let [result (get-element-csss
                  *driver*
                  {:id :div-css-simple}
                  :display :background-color "width" "height")
          [display background-color width height] result]
      (is (= display "block"))
      (is (or (= background-color "rgb(204, 204, 204)")
              (= background-color "rgba(204, 204, 204, 1)")))
      (is (= width "150px"))
      (is (= height "250px"))))
  (testing "styled css"
    (let [result (get-element-csss
                  *driver*
                  {:id :div-css-styled}
                  :display :width :height)
          [display width height] result]
      (is (= display "block"))
      (is (= width "333px"))
      (is (= height "111px"))))
  (testing "missing css"
    (let [result (get-element-csss
                  *driver*
                  {:id :div-css-styled}
                  :foo :bar "baz")]
      (is (every? nil? result)))))

(deftest test-wait-text
  (testing "wait for text simple"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end})
      (click {:id :wait-button})
      (wait-has-text :wait-span "-secret-" {:message "wait simiple"}))
    (is true "text found"))
  (testing "wait for text timeout"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end})
      (click {:id :wait-button}))
    (try+
     (wait-has-text *driver*
                    :wait-span
                    "-secret-"
                    {:timeout 1
                     :message "No -secret- text on the page"})
     (is false "should not be executd")
     (catch [:type :etaoin/timeout] data
       (is (= (-> data (dissoc :predicate :time-rest))
              {:type :etaoin/timeout
               :message "No -secret- text on the page"
               :timeout 1
               :interval 0.1
               :times 11})))))
  (testing "wait for non-existing text"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end}))
    (try+
     (wait-has-text *driver*
                    :wait-span
                    "-dunno-whatever-foo-bar-"
                    {:timeout 2
                     :message "wait non-existing"})
     (is false "should not be executed")
     (catch [:type :etaoin/timeout] data
       (is (= (-> data (dissoc :predicate :time-rest))
              {:type :etaoin/timeout
               :message "wait non-existing"
               :timeout 2
               :interval 0.1
               :times 20}))))))

(deftest test-wait-has-class
  (is 1)
  (testing "wait for an element has class"
    (doto *driver*
      (click {:id :wait-add-class-trigger})
      (wait-has-class {:id :wait-add-class-target}
                      :new-one
                      {:timeout 20
                       :interval 1
                       :message "No 'new-one' class found."}))))

(deftest test-close-window
  (is 1)
  (doto *driver*
    (close-window)))

(deftest test-drag-n-drop
  (is 1)
  (let [url "http://marcojakob.github.io/dart-dnd/basic/web/"
        doc {:class :document}
        trash {:xpath "//div[contains(@class, 'trash')]"}]
    (when-not (or (firefox? *driver*)
                  (safari? *driver*))
      (doto *driver*
        (go url)
        (drag-and-drop doc trash)
        (drag-and-drop doc trash)
        (drag-and-drop doc trash)
        (drag-and-drop doc trash)
        (-> (absent? doc) is)))))

(deftest test-element-location
  (let [q {:id :el-location-input}
        loc (get-element-location *driver* q)
        {:keys [x y]} loc]
    (is (numeric? x))
    (is (numeric? y))))

;; Here and below: when running a Safari driver,
;; you need to unplug your second monitor. That sounds crazy,
;; I know. Bun nevertheless, if a Safari window appears on the second
;; monitor, the next two test will fail due to window error.

(deftest test-window-position
  (let [{:keys [x y]} (get-window-position *driver*)]
    (is (numeric? x))
    (is (numeric? y))
    (set-window-position *driver* (+ x 10) (+ y 10))
    (let [{:keys [x' y']} (get-window-position *driver*)]
      (is (not= x x'))
      (is (not= y y')))))

(deftest test-window-size
  (testing "getting size"
    (let [{:keys [width height]} (get-window-size *driver*)]
      (is (numeric? width))
      (is (numeric? height))
      (set-window-size *driver* (+ width 10) (+ height 10))
      (let [{:keys [width' height']} (get-window-size *driver*)]
        (not= width width')
        (not= height height')))))

(deftest test-active-element
  (testing "active element"
    (when-not-safari *driver*
      (doto *driver*
        (click {:id :set-active-el})
        (-> (get-element-attr :active :id)
            (= "active-el-input")
            is)))
    (when-safari *driver*
      (is 1))))

(deftest test-element-text
  (let [text (get-element-text *driver* {:id :element-text})]
    (is (= text "Element text goes here."))))

(deftest test-cookies
  (testing "getting all cookies"
    (let [cookies (get-cookies *driver*)]
      (when-safari *driver*
        (is (= cookies
               [{:domain ".^filecookies^"
                 :secure false
                 :expiry 0
                 :httpOnly false
                 :value "test1"
                 :path "/"
                 :name "cookie1"}
                {:domain ".^filecookies^"
                 :secure false
                 :expiry 0
                 :httpOnly false
                 :value "test2"
                 :path "/"
                 :name "cookie2"}])))
      (when-chrome *driver*
        (is (= cookies [])))
      (when-firefox *driver*
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
      (when-phantom *driver*
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
  (testing "getting a cookie"
    (let [cookie (get-cookie *driver* :cookie2)]
      (when-safari *driver*
        (is (= cookie
               {:domain ".^filecookies^"
                :secure false
                :expiry 0
                :httpOnly false
                :value "test2"
                :path "/"
                :name "cookie2"})))
      (when-chrome *driver*
        (is (nil? cookie)))
      (when-firefox *driver*
        (is (= cookie
               {:name "cookie2"
                :value "test2"
                :path "/"
                :domain ""
                :expiry nil
                :secure false
                :httpOnly false})))
      (when-phantom *driver*
        (is (= cookie
               {:domain ""
                :httponly false
                :name "cookie2"
                :path "/"
                :secure false
                :value "test2"})))))
  (testing "setting a cookie"
    (when-not (or (phantom? *driver*)
                  (safari? *driver*))
      (set-cookie *driver* {:httponly false
                            :name "cookie3"
                            :domain ""
                            :secure false
                            :value "test3"})
      (when-firefox *driver*
        (let [cookie (get-cookie *driver* :cookie3)]
          (is (= cookie)
              {:name "cookie3"
               :value "test3"
               :path ""
               :domain ""
               :expiry nil
               :secure false
               :httpOnly false})))))
  (testing "deleting a cookie"
    (when-not-phantom
        *driver*
      (delete-cookie *driver* :cookie3)
      (let [cookie (get-cookie *driver* :cookie3)]
        (is (nil? cookie)))))
  (testing "deleting all cookies"
    (doto *driver*
      delete-cookies
      (-> get-cookies
          (= [])
          is))))

(deftest test-page-source
  (let [src (get-source *driver*)]
    (if (or (firefox? *driver*)
            (safari? *driver*))
      (is (str/starts-with? src "<html><head>"))
      (is (str/starts-with? src "<!DOCTYPE html>")))))

(deftest test-screenshot
  (with-tmp-file "screenshot" ".png" path
    (screenshot *driver* path)
    (-> path
        io/file
        ImageIO/read
        is)))

(deftest test-js-execute
  (testing "simple result"
    (let [result (js-execute *driver* "return 42;")]
      (is (= result 42))))
  (testing "with args"
    (let [script "return {foo: arguments[0], bar: arguments[1]};"
          result (js-execute *driver* script {:test 42} [true, nil, "Hello"])]
      (is (= result
             {:foo {:test 42}
              :bar [true nil "Hello"]})))))

(deftest test-add-script
  (let [js-url (-> "js/inject.js" io/resource str)]
    (testing "adding a script"
      (add-script *driver* js-url)
      (let [result (js-execute *driver* "return injected_func();")]
        (is (= result "I was injected"))))))

(deftest test-set-hash
  (testing "set hash"
    (doto *driver*
      (set-hash "hello")
      (-> get-hash (= "hello") is)
      (-> get-url (str/ends-with? "/test.html#hello") is)
      (set-hash "goodbye")
      (-> get-url (str/ends-with? "/test.html#goodbye") is))))

(deftest test-find-element
  (let [text (get-element-text *driver* {:class :target})]
    (is (= text "target-1")))
  (let [text (get-element-text *driver* [{:class :foo}
                                         {:class :target}])]
    (is (= text "target-2")))
  (with-xpath *driver*
    (let [text (get-element-text *driver* ".//div[@class='target'][1]")]
      (is (= text "target-1"))))
  (let [text (get-element-text *driver* {:css ".target"})]
    (is (= text "target-1")))
  (let [q [{:css ".bar"} ".//div[@class='inside']" {:tag :span}]
        text (get-element-text *driver* q)]
    (is (= text "target-3"))))

(deftest test-find-elements
  (testing "simple case"
    (let [q {:class :find-elements-target}
          elements (query-all *driver* q)]
      (is (= (count elements) 4))))
  (testing "nested case"
    (let [q [{:id :find-elements-nested}
             {:class :nested}
             {:class :target}]
          elements (query-all *driver* q)
          texts (for [el elements]
                  (get-element-text-el *driver* el))]
      (is (= (count elements) 2))
      (is (= texts ["1" "2"])))))

(deftest test-multiple-elements
  (testing "tag names"
    (let [q {:xpath ".//div[@id='operate-multiple-elements']//*"}
          elements (query-all *driver* q)
          tag-names (for [el elements]
                      (get-element-tag-el *driver* el))]
      (is (= (vec tag-names)
             ["div" "b" "p" "span"])))))

(deftest test-postmortem
  (let [dir-tmp (format
                 "%s/%s"
                 (System/getProperty "java.io.tmpdir")
                 (System/currentTimeMillis))]
    (io/make-parents (format "%s/%s" dir-tmp "_"))
    (testing "postmortem"
      (try
        (with-postmortem *driver* {:dir dir-tmp}
          (click *driver* :non-existing-element))
        (is false "should be caught")
        (catch Exception e
          (is true "caught")
          (let [files (file-seq (io/file dir-tmp))]
            (is (= (-> files rest count)
                   2))))))))

(deftest test-click-by-text
  (testing "click on a button by text")
  (doto *driver*
    (refresh)
    (click-visible {:tag :button :text "Test Wait Button"})
    (wait-has-text :wait-span "-secret-" {:message "wait simiple"})))
