(ns etaoin.api-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [slingshot.slingshot :refer [try+]])
  (:import javax.imageio.ImageIO))

(defmacro with-tmp-file [prefix suffix bind & body]
  `(let [tmp#  (java.io.File/createTempFile ~prefix ~suffix)
         ~bind (.getAbsolutePath tmp#)]
     (try
       ~@body
       (finally
         (.delete tmp#)))))

(defn numeric? [val]
  (or (instance? Double val)
      (instance? Integer val)))

;; By default we run the tests with all the drivers supported on the current OS.
;; To override this, you can set the environment variable ETAOIN_TEST_DRIVERS
;; to a Clojure vector encoded as a string; for example:
;;
;;  ETAOIN_TEST_DRIVERS="[:firefox]" lein test

(defn get-drivers-from-env []
  (when-let [override (System/getenv "ETAOIN_TEST_DRIVERS")]
    (edn/read-string override)))

(defn get-drivers-from-prop []
  (case (first (str/split (System/getProperty "os.name") #"\s+"))
    "Linux"   [:firefox :chrome :phantom]
    "Mac"     [:chrome :edge :firefox :phantom :safari]
    "Windows" [:firefox :chrome :edge :phantom :safari]
    nil))

(defn get-default-drivers []
  [:firefox :chrome :phantom :safari])

(def default-opts
  {:chrome  {:args ["--no-sandbox"]}
   :firefox {}
   :safari  {:path-driver "/Applications/Safari Technology Preview.app/Contents/MacOS/safaridriver"}
   :edge    {:args ["--headless"]}})

(def drivers
  (or (get-drivers-from-env)
      (get-drivers-from-prop)
      (get-default-drivers)))

(def ^:dynamic *driver*)

;; tests failed in safari 13.1.1 https://bugs.webkit.org/show_bug.cgi?id=202589 use STP newest
(defn fixture-browsers [f]
  (let [url (-> "html/test.html" io/resource str)]
    (doseq [type drivers]
      (with-driver type (get default-opts type {}) driver
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

(deftest test-select
  (testing "test `select` on select-box"
    (let [default-val  (get-element-value *driver* :simple-country)
          _            (select *driver* :simple-country "France")
          selected-val (get-element-value *driver* :simple-country)]
      (is (= "rf" default-val))
      (is (= "fr" selected-val)))))

(deftest test-input
  (testing "fill multiple imputs"
    (doto *driver*
      (fill-multi {:simple-input    1
                   :simple-password 2
                   :simple-textarea 3})
      (click :simple-submit)
      (when-safari (wait 3))
      (-> get-url
          (str/ends-with? "?login=1&password=2&message=3")
          is)))
  (testing "fill human multiple imputs"
    (doto *driver*
      (fill-human-multi {:simple-input    "login"
                         :simple-password "123"
                         :simple-textarea "text"})
      (click :simple-submit)
      (when-safari (wait 3))
      (-> get-url
          (str/ends-with? "?login=login&password=123&message=text")
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
      (fill-multi {:simple-input    1
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
  (when-not-phantom  *driver*
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

(deftest test-properties
  (when-firefox *driver*
    (let [result (get-element-properties
                   *driver*
                   :input-property
                   :value)]
      (is (= ["val"] result)))))

(deftest test-element-value
  (let [result (get-element-value
                 *driver*
                 :input-property)]
    (is (= "val" result))))

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
    (let [val (get-element-attr *driver*
                                {:id :input-attr}
                                :onclick)]
      (is (= val "alert(123)"))))
  (testing "missing attributes"
    (doto *driver*
      (-> (get-element-attrs
            {:id :input-attr}
            :foo "bar" :baz "dunno")
          (= [nil nil nil nil])
          is))))

(deftest test-get-inner-html
  (let [inner-html "<div>Inner HTML</div>"
        result     (get-element-inner-html *driver* :element-props)]
    (is (= inner-html result))))

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

(deftest test-wait-has-text
  (testing "wait for text simple"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end})
      (click {:id :wait-button})
      (wait-has-text :wait-span "-secret-"))
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
                     {:timeout 1})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait for :wait-span element has text -secret-"
                :timeout  1
                :interval 0.33
                :times    4})))))
  (testing "wait for non-existing text"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end}))
    (try+
      (wait-has-text *driver*
                     :wait-span
                     "-dunno-whatever-foo-bar-"
                     {:timeout 2})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait for :wait-span element has text -dunno-whatever-foo-bar-"
                :timeout  2
                :interval 0.33
                :times    7}))))))

(deftest test-wait-has-text-everywhere
  (testing "wait for text simple"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end})
      (click {:id :wait-button})
      (wait-has-text-everywhere "-secret-"))
    (is true "text found"))
  (testing "wait for text timeout"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end})
      (click {:id :wait-button}))
    (try+
      (wait-has-text-everywhere *driver*
                                "-secret-"
                                {:timeout 1})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait for {:xpath \"*\"} element has text -secret-"
                :timeout  1
                :interval 0.33
                :times    4})))))
  (testing "wait for non-existing text"
    (doto *driver*
      (refresh)
      (wait-visible {:id :document-end}))
    (try+
      (wait-has-text-everywhere *driver*
                                "-dunno-whatever-foo-bar-"
                                {:timeout 2})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait for {:xpath \"*\"} element has text -dunno-whatever-foo-bar-"
                :timeout  2
                :interval 0.33
                :times    7}))))))

(deftest test-wait-has-class
  (is 1)
  (testing "wait for an element has class"
    (doto *driver*
      (scroll-query :wait-add-class-trigger)
      (click :wait-add-class-trigger)
      (wait-has-class :wait-add-class-target
                      :new-one
                      {:timeout  20
                       :interval 1
                       :message  "No 'new-one' class found."}))))

(deftest test-close-window
  (is 1)
  (doto *driver*
    (close-window)))

(deftest test-drag-n-drop
  (is 1)
  (let [url   (-> "html/drag-n-drop/index.html" io/resource str)
        doc   {:class :document}
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
  (let [q             {:id :el-location-input}
        loc           (get-element-location *driver* q)
        {:keys [x y]} loc]
    (is (numeric? x))
    (is (numeric? y))))

;; Here and below: when running a Safari driver,
;; you need to unplug your second monitor. That sounds crazy,
;; I know. Bun nevertheless, if a Safari window appears on the second
;; monitor, the next two test will fail due to window error.

(deftest test-window-position
  (when-not-drivers
      [:phantom :edge] *driver*
      (let [{:keys [x y]} (get-window-position *driver*)]
        (is (numeric? x))
        (is (numeric? y))
        (set-window-position *driver* (+ x 10) (+ y 10))
        (let [{x' :x y' :y} (get-window-position *driver*)]
          (is (not= x x'))
          (is (not= y y'))))))

(deftest test-window-size
  (testing "getting size"
    (let [{:keys [width height]} (get-window-size *driver*)]
      (is (numeric? width))
      (is (numeric? height))
      (set-window-size *driver* (+ width 10) (+ height 10))
      (let [{width' :width height' :height} (get-window-size *driver*)]
        (is (not= width width'))
        (is (not= height height'))))))

(deftest test-switch-window
  (let [init-handle   (get-window-handle *driver*)
        init-url      (get-url *driver*)
        _             (click *driver* :switch-window)
        new-handles   (get-window-handles *driver*)
        new-handle    (first (filter #(not= % init-handle) new-handles))
        _             (switch-window *driver* new-handle)
        target-handle (get-window-handle *driver*)
        target-url    (get-url *driver*)]
    (is (not= init-handle target-handle))
    (is (= 2 (count new-handles)))
    (is (= new-handle target-handle))
    (is (not= init-url target-url))))

;; need refactoring not working for headless & firefox
#_
(deftest test-maximize
  (when-not-headless *driver*
    (let [{:keys [x y]}          (get-window-position *driver*)
          {:keys [width height]} (get-window-size *driver*)]
      (maximize *driver*)
      (let [{x' :x y' :y}                   (get-window-position *driver*)
            {width' :width height' :height} (get-window-size *driver*)]
        (is (not= x x'))
        (is (not= y y'))
        (is (not= width width'))
        (is (not= height height'))))))

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

(deftest test-element-size
  (let [{:keys [width height]} (get-element-size *driver* {:id :element-text})]
    (is (numeric? width))
    (is (numeric? height))))

(deftest test-cookies
  (testing "getting all cookies"
    (let [cookies    (get-cookies *driver*)
          cookies-ff (map #(dissoc % :sameSite) cookies)]
      (when-safari *driver*
        (is (= cookies
               [{:domain   ".^filecookies^"
                 :secure   false
                 :httpOnly false
                 :value    "test1"
                 :path     "/"
                 :name     "cookie1"}
                {:domain   ".^filecookies^"
                 :secure   false
                 :httpOnly false
                 :value    "test2"
                 :path     "/"
                 :name     "cookie2"}])))
      (when-chrome *driver*
        (is (= cookies [])))
      (when-firefox *driver*
        (is (= cookies-ff [{:name     "cookie1",
                            :value    "test1",
                            :path     "/",
                            :domain   "",
                            :secure   false,
                            :httpOnly false}
                           {:name     "cookie2",
                            :value    "test2",
                            :path     "/",
                            :domain   "",
                            :secure   false,
                            :httpOnly false}])))
      (when-phantom *driver*
        (is (= cookies [{:domain   "",
                         :httponly false,
                         :name     "cookie2",
                         :path     "/",
                         :secure   false,
                         :value    "test2"}
                        {:domain   "",
                         :httponly false,
                         :name     "cookie1",
                         :path     "/",
                         :secure   false,
                         :value    "test1"}])))))
  (testing "getting a cookie"
    (let [cookie    (get-cookie *driver* :cookie2)
          cookie-ff (dissoc cookie :sameSite)]
      (when-safari *driver*
        (is (= cookie
               {:domain   ".^filecookies^"
                :secure   false
                :httpOnly false
                :value    "test2"
                :path     "/"
                :name     "cookie2"})))
      (when-chrome *driver*
        (is (nil? cookie)))
      (when-firefox *driver*
        (is (= cookie-ff
               {:name     "cookie2"
                :value    "test2"
                :path     "/"
                :domain   ""
                :secure   false
                :httpOnly false})))
      (when-phantom *driver*
        (is (= cookie
               {:domain   ""
                :httponly false
                :name     "cookie2"
                :path     "/"
                :secure   false
                :value    "test2"})))))
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
    (if (phantom? *driver*)
      (is (str/starts-with? src "<!DOCTYPE html>"))
      (is (str/starts-with? src "<html><head>")))))

(deftest test-screenshot
  (with-tmp-file "screenshot" ".png" path
    (screenshot *driver* path)
    (-> path
        io/file
        ImageIO/read
        is)))

(deftest test-screenshot-element
  (when (or (chrome? *driver*)
            (firefox? *driver*))
    (with-tmp-file "screenshot" ".png" path
      (screenshot-element *driver* {:id :css-test} path)
      (-> path
          io/file
          ImageIO/read
          is))))

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
      (wait 1)
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
  (let [q    [{:css ".bar"} ".//div[@class='inside']" {:tag :span}]
        text (get-element-text *driver* q)]
    (is (= text "target-3"))))

(deftest test-find-elements-more
  (testing "simple case"
    (let [q        {:class :find-elements-target}
          elements (query-all *driver* q)]
      (is (= (count elements) 4))))
  (testing "nested case"
    (let [q        [{:id :find-elements-nested}
                    {:class :nested}
                    {:class :target}]
          elements (query-all *driver* q)
          texts    (for [el elements]
                     (get-element-text-el *driver* el))]
      (is (= (count elements) 2))
      (is (= texts ["1" "2"])))))

(deftest test-multiple-elements
  (testing "tag names"
    (let [q         {:xpath ".//div[@id='operate-multiple-elements']//*"}
          elements  (query-all *driver* q)
          tag-names (for [el elements]
                      (str/lower-case (get-element-tag-el *driver* el)))]
      (is (= (vec tag-names)
             ["div" "b" "p" "span"])))))

(deftest test-query-tree
  (let [url            (-> "html/test2.html" io/resource str)
        _              (go *driver* url)
        all-div        (query-tree *driver* {:tag :div})
        all-li         (query-tree *driver* {:tag :li})
        li-three-level (query-tree
                         *driver* {:tag :div} {:tag :div} {:tag :div} {:tag :li})
        tag-a          (query-tree *driver* {:tag :div} {:tag :div} {:tag :a})]
    (is (= 6 (count all-div)))
    (is (= 8 (count all-li)))
    (is (= 5 (count li-three-level)))
    (is (= 1 (count tag-a)))))

(deftest test-child
  (let [parent-el (query *driver* {:css "#wc3-barks"})
        child-el  (child *driver* parent-el {:css ".crypt-lord"})
        tag-name  (str/lower-case (get-element-tag-el *driver* child-el))
        tag-text  (get-element-text-el *driver* child-el)]
    (is (= "span" tag-name))
    (is (str/includes? tag-text "From the depths I've come!"))))

(deftest test-children
  (let [parent-el      (query *driver* {:css "#wc3-barks"})
        children-els   (children *driver* parent-el {:css "p"})
        children-texts (map #(get-element-text-el *driver* %) children-els)]
    (is (= ["p" "p"] (map #(str/lower-case (get-element-tag-el *driver* %)) children-els)))
    (is (str/includes? (first children-texts) "From the depths I've come!"))
    (is (str/includes? (last children-texts) "I've come from the darkness of the pit!"))))

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
          (let [files               (file-seq (io/file dir-tmp))
                expected-file-count (if (supports-logs? *driver*) 3 2)]
            (is (= (-> files rest count)
                   expected-file-count))))))))

(deftest test-find-quotes-in-text
  (doto *driver*
    (-> (has-text? "'quote") is)))

(deftest test-has-text
  (testing "test :fn/has-string"
    (is (boolean (query *driver* {:fn/has-string "From the depth"}))))
  (testing "gloval"
    (is (has-text? *driver* "From the depths I've come!"))
    (is (has-text? *driver* "I've come from the dark")))
  (testing "relative"
    (is (has-text? *driver* [:wc3-barks {:tag :p} {:tag :span}] "ths I've come!")))
  (testing "short path"
    (is (has-text? *driver* [:wc3-barks {:tag :span}] "ths I've")))
  (testing "wrong path"
    (is (not (has-text? *driver* [:wc3-barks {:tag :p} :pit-lord] "ths I've come!")))))


;; actions

(deftest test-actions
  (testing "input key and mouse click"
    (when-not-phantom *driver*
      (let [input    (query *driver* :simple-input)
            password (query *driver* :simple-password)
            textarea (query *driver* :simple-textarea)
            submit   (query *driver* :simple-submit)
            keyboard (-> (make-key-input)
                         add-double-pause
                         (with-key-down "\uE01B")
                         add-double-pause
                         (with-key-down "\uE01C")
                         add-double-pause
                         (with-key-down "\uE01D"))
            mouse    (-> (make-mouse-input)
                         (add-pointer-click-el input)
                         add-pause
                         (add-pointer-click-el password)
                         add-pause
                         (add-pointer-click-el textarea)
                         add-pause
                         (add-pointer-click-el submit))]
        (perform-actions *driver* keyboard mouse)
        (wait 1)
        (is (str/ends-with? (get-url *driver*) "?login=1&password=2&message=3")))))
  (testing "drag-n-drop"
    (when-not-phantom *driver*
      (let [drag-and-drop (fn [driver q-from q-to]
                            (let [el-from (query driver q-from)
                                  el-to   (query driver q-to)
                                  mouse   (-> (make-mouse-input)
                                              (add-pointer-move-to-el el-from)
                                              (with-pointer-left-btn-down
                                                (add-pointer-move-to-el el-to)))]
                              (perform-actions driver mouse)))
            url           (-> "html/drag-n-drop/index.html" io/resource str)
            doc           {:class :document}
            trash         {:xpath "//div[contains(@class, 'trash')]"}]
        (doto *driver*
          (go url)
          (drag-and-drop doc trash)
          (drag-and-drop doc trash)
          (drag-and-drop doc trash)
          (drag-and-drop doc trash)
          (-> (absent? doc) is))))))
