(ns etaoin.api-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [etaoin.api :as e]
   [etaoin.impl.util :as util]
   [etaoin.impl.client :as client]
   [etaoin.keys :as k]
   [etaoin.test-report :as test-report]
   [slingshot.slingshot :refer [try+]])
  (:import [java.net ServerSocket]))

(defn numeric? [val]
  (or (instance? Double val)
      (instance? Integer val)))

;; By default we run the tests with all the drivers supported on the current OS.
;; To override this, you can set the environment variable ETAOIN_TEST_DRIVERS
;; to a Clojure vector encoded as a string; see script/test.clj for how we use this.

(defn get-drivers-from-env []
  (when-let [override (System/getenv "ETAOIN_TEST_DRIVERS")]
    (edn/read-string override)))

(defn os-name []
  (first (str/split (System/getProperty "os.name") #"\s+")))

(defn get-drivers-from-prop []
  (case (os-name)
    "Linux"   [:firefox :chrome]
    "Mac"     [:chrome :edge :firefox :safari]
    "Windows" [:firefox :chrome :edge]
    nil))

(defn get-default-drivers []
  [:firefox :chrome :safari])

(defn ci? [] (System/getenv "CI"))

(def default-opts
  {:chrome  {}
   :firefox (cond-> {}
              ;; add logging for typically flaky CI scenario
              (and (ci?) (fs/windows?)) (merge {:log-stdout :inherit
                                                :log-stderr :inherit
                                                :driver-log-level "info"}))
   :safari (cond-> {}
             ;; add logging for kind flaky CI scenario (maybe we'll answer why we need
             ;; to retry launching safaridriver automatically)
             ;; safaridriver only logs details to a somewhat obscure file, will follow up
             ;; with some technique to discover/dump this file
             (ci?) (merge {:log-stdout :inherit :log-stderr :inherit}))
   :edge    {:args ["--headless"]}})

(def drivers
  (or (get-drivers-from-env)
      (get-drivers-from-prop)
      (get-default-drivers)))

(def ^:dynamic *driver* nil)

(defn- find-available-port []
  (with-open [sock (ServerSocket. 0)]
    (.getLocalPort sock)))

(def ^:dynamic *test-server-port* nil)

(defn test-server-url [path]
  (format "http://localhost:%d/%s" *test-server-port* path))

;; tests failed in safari 13.1.1 https://bugs.webkit.org/show_bug.cgi?id=202589 use STP newest
(defn fixture-browsers [f]
  (let [url (test-server-url "test.html")]
    (doseq [type drivers
            :let [opts (get default-opts type {})]]
      (e/with-driver type opts driver
        (e/go driver url)
        (e/wait-visible driver {:id :document-end})
        (binding [*driver* driver
                  test-report/*context* (name type)]
          (testing (name type)
            (f)))))))

(use-fixtures
  :each
  fixture-browsers)

(defn report-browsers [f]
  (println "Testing with browsers:" drivers)
  (f))

(defn test-server [f]
  (binding [*test-server-port* (find-available-port)]
    (let [proc (p/process {:out :inherit :err :inherit}
                          "bb test-server --port" *test-server-port*)]
      (let [deadline (+ (System/currentTimeMillis) 15000)
            test-url (test-server-url "test.html") ]
        (loop []
          (let [resp (try (client/http-request {:method :get :url test-url})
                          (catch Throwable _ :not-ready))]
            (when (= :not-ready resp)
              (if (< (System/currentTimeMillis) deadline)
                (do
                  (println "- waiting for test-server to be ready at" test-url)
                  (Thread/sleep 1000)
                  (recur))
                (throw (ex-info "Timed out waiting for ready test server" {}))))))
        (println "Test server ready"))
      (f)
      (p/destroy proc)
      @proc)))

(use-fixtures
  :once
  report-browsers
  test-server)

(deftest test-visible
  (doto *driver*
    (-> (e/visible? {:id :button-visible}) is)
    (-> (e/invisible? {:id :button-hidden}) is)
    (-> (e/invisible? {:id :div-hidden}) is)
    (-> (e/invisible? {:id :dunno-foo-bar}) is)))

(deftest test-select
  (testing "test `select` on select-box"
    (let [default-val  (e/get-element-value *driver* :simple-country)
          _            (e/select *driver* :simple-country "France")
          selected-val (e/get-element-value *driver* :simple-country)]
      (is (= "rf" default-val))
      (is (= "fr" selected-val)))))

(deftest test-multiple-click
  (e/click-multi *driver* [:vehicle1 :vehicle2 :vehicle3] 0.3)
  (is (e/selected? *driver* :vehicle1))
  (is (e/selected? *driver* :vehicle2))
  (is (e/selected? *driver* :vehicle3)))

(deftest test-input
  (testing "fill multiple inputs"
    (doto *driver*
      (e/fill-multi {:simple-input    1
                     :simple-password 2
                     :simple-textarea 3})
      (e/click :simple-submit)
      (e/when-safari (e/wait 3))
      (-> e/get-url
          (str/ends-with? "?login=1&password=2&message=3")
          is)))
  (testing "fill human multiple inputs"
    (doto *driver*
      (e/fill-human-multi {:simple-input    "login"
                           :simple-password "123"
                           :simple-textarea "text"})
      (e/click :simple-submit)
      (e/when-safari (e/wait 3))
      (-> e/get-url
          (str/ends-with? "?login=login&password=123&message=text")
          is)))
  (testing "fill multiple vars"
    (doto *driver*
      (e/fill :simple-input 1 "test" 2 \space \A)
      (e/click :simple-submit)
      (e/when-safari (e/wait 3))
      (-> e/get-url
          (str/ends-with? "?login=1test2+A&password=&message=")
          is))))

(deftest test-unicode-bmp-input
  (let [data {:simple-input "ĩṋṗṵţ"
              :simple-password "ǷḁṡṢẅỏṟƉ"
              :simple-textarea "т️ẹẍṱảṙẸẚ"}]
    (testing "fill-multi"
      (e/fill-multi *driver* data)
      (doseq [f [:simple-input :simple-password :simple-textarea]]
        (is (= (f data) (e/get-element-value *driver* f)))))
    (testing "fill-human-multi"
      (e/refresh *driver*)
      (e/fill-human-multi *driver* data)
      (doseq [f [:simple-input :simple-password :simple-textarea]]
        (is (= (f data) (e/get-element-value *driver* f)))))))

(deftest test-unicode-above-bmp-input
    ;; as of 2023-04-29 not supported on chrome and edge
    ;; https://bugs.chromium.org/p/chromedriver/issues/detail?id=2269
    (when-not (#{:chrome :edge} (e/dispatch-driver *driver*))
      (let [data {:simple-input "😊🍂input🍃"
                  :simple-password "🔆password☠️ "
                  :simple-textarea "🎉🚀textarea👀☀️"}]
        (testing "fill-multi"
          (e/fill-multi *driver* data)
          (doseq [f [:simple-input :simple-password :simple-textarea]]
            (is (= (f data) (e/get-element-value *driver* f)))))
        (testing "fill-human-multi"
          (e/refresh *driver*)
          (e/fill-human-multi *driver* data)
          (doseq [f [:simple-input :simple-password :simple-textarea]]
            (is (= (f data) (e/get-element-value *driver* f))))))))

(deftest test-clear
  (testing "simple clear"
    (doto *driver*
      (e/fill {:id :simple-input} "test")
      (e/clear {:id :simple-input})
      (e/click {:id :simple-submit})
      (e/when-safari (e/wait 3))
      (-> e/get-url
          (str/ends-with? "?login=&password=&message=")
          is)))

  (testing "multiple clear"
    (doto *driver*
      (e/fill-multi {:simple-input    1
                     :simple-password 2
                     :simple-textarea 3})
      (e/clear :simple-input
               :simple-password
               :simple-textarea)
      (e/when-safari (e/wait 3))
      (-> e/get-url
          (str/ends-with? "?login=&password=&message=")
          is))))

(deftest test-enabled
  (doto *driver*
    (-> (e/disabled? {:id :input-disabled}) is)
    (-> (e/enabled? {:id :input-not-disabled}) is)
    (-> (e/disabled? {:id :textarea-disabled}) is))
  (is (thrown?
        clojure.lang.ExceptionInfo
        (e/enabled? *driver* {:id :dunno-foo-bar}))))

(deftest test-exists
  (doto *driver*
    (-> (e/exists? {:tag :html}) is)
    (-> (e/exists? {:tag :body}) is)
    (-> (e/absent? {:id :dunno-foo-bar}) is)))

;; In Safari, alerts work quite slow, so we add some delays.
(deftest test-alert
  (e/when-not-phantom  *driver*
    (doto *driver*
      (e/click {:id :button-alert})
      (e/when-safari (e/wait 1))
      (-> e/get-alert-text (= "Hello!") is)
      (-> e/has-alert? is)
      (e/accept-alert)
      (e/when-safari (e/wait 1))
      (-> e/has-alert? not is)
      (e/click {:id :button-alert})
      (e/when-safari (e/wait 1))
      (-> e/has-alert? is)
      (e/dismiss-alert)
      (e/when-safari (e/wait 1))
      (-> e/has-alert? not is))))

(deftest test-properties
  (e/when-firefox *driver*
    (let [result (e/get-element-properties
                   *driver*
                   :input-property
                   :value)]
      (is (= ["val"] result)))))

(deftest test-element-value
  (let [result (e/get-element-value
                 *driver*
                 :input-property)]
    (is (= "val" result))))

(deftest test-attributes
  (testing "common attributes"
    (doto *driver*
      (-> (e/get-element-attrs
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
    (let [val (e/get-element-attr *driver*
                                  {:id :input-attr}
                                  :onclick)]
      (is (= val "alert(123)"))))
  (testing "missing attributes"
    (doto *driver*
      (-> (e/get-element-attrs
            {:id :input-attr}
            :foo "bar" :baz "dunno")
          (= [nil nil nil nil])
          is))))

(deftest test-get-inner-html
  (let [inner-html "<div>Inner HTML</div>"
        result     (e/get-element-inner-html *driver* :element-props)]
    (is (= inner-html result))))

(deftest test-title
  (doto *driver*
    (-> e/get-title (= "Webdriver Test Document") is)))

(deftest test-url
  (doto *driver*
    (-> e/get-url
        (str/ends-with? "/test.html")
        is)))

(deftest test-css-props
  (testing "single css"
    (doto *driver*
      (-> (e/get-element-css {:id :div-css-simple} :display)
          (= "block")
          is)))
  (testing "multiple css"
    (let [result (e/get-element-csss
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
    (let [result (e/get-element-csss
                   *driver*
                   {:id :div-css-styled}
                   :display :width :height)
          [display width height] result]
      (is (= display "block"))
      (is (= width "333px"))
      (is (= height "111px"))))
  (testing "missing css"
    (let [result (e/get-element-csss
                   *driver*
                   {:id :div-css-styled}
                   :foo :bar "baz")]
      (is (every? nil? result)))))

(deftest test-wait-has-text
  (testing "wait for text simple"
    (doto *driver*
      (e/refresh)
      (e/wait-visible {:id :document-end})
      (e/click {:id :wait-button})
      (e/wait-has-text :wait-span "-secret-")))
  (testing "wait for text timeout"
    (doto *driver*
      (e/refresh)
      (e/wait-visible {:id :document-end})
      (e/click {:id :wait-button}))
    (try+
      (e/wait-has-text *driver*
                       :wait-span
                       "-secret-"
                       {:timeout 1})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait until :wait-span element has text -secret-"
                :timeout  1
                :interval 0.33
                :times    4})))))
  (testing "wait for non-existing text"
    (doto *driver*
      (e/refresh)
      (e/wait-visible {:id :document-end}))
    (try+
      (e/wait-has-text *driver*
                       :wait-span
                       "-dunno-whatever-foo-bar-"
                       {:timeout 2})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait until :wait-span element has text -dunno-whatever-foo-bar-"
                :timeout  2
                :interval 0.33
                :times    7}))))))

(deftest test-wait-has-text-everywhere
  (testing "wait for text simple"
    (doto *driver*
      (e/refresh)
      (e/wait-visible {:id :document-end})
      (e/click {:id :wait-button})
      (e/wait-has-text-everywhere "-secret-")))
  (testing "wait for text timeout"
    (doto *driver*
      (e/refresh)
      (e/wait-visible {:id :document-end})
      (e/click {:id :wait-button}))
    (try+
      (e/wait-has-text-everywhere *driver*
                                  "-secret-"
                                  {:timeout 1})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait until {:xpath \"*\"} element has text -secret-"
                :timeout  1
                :interval 0.33
                :times    4})))))
  (testing "wait for non-existing text"
    (doto *driver*
      (e/refresh)
      (e/wait-visible {:id :document-end}))
    (try+
      (e/wait-has-text-everywhere *driver*
                                  "-dunno-whatever-foo-bar-"
                                  {:timeout 2})
      (is false "should not be executed")
      (catch [:type :etaoin/timeout] data
        (is (= (-> data (dissoc :predicate :time-rest))
               {:type     :etaoin/timeout
                :message  "Wait until {:xpath \"*\"} element has text -dunno-whatever-foo-bar-"
                :timeout  2
                :interval 0.33
                :times    7}))))))

(deftest test-wait-has-class
  (testing "wait for an element has class"
    (doto *driver*
      (e/scroll-query :wait-add-class-trigger)
      (e/click :wait-add-class-trigger)
      (e/wait-has-class :wait-add-class-target
                      :new-one
                      {:timeout  20
                       :interval 1
                       :message  "No 'new-one' class found."}))))

(deftest test-close-window
  (doto *driver*
    (e/close-window)))

(deftest test-drag-n-drop
  (let [url   (test-server-url "drag-n-drop/index.html")
        doc   {:class :document}
        trash {:xpath "//div[contains(@class, 'trash')]"}]
    (doto *driver*
      (e/go url)
      (e/drag-and-drop doc trash)
      (e/drag-and-drop doc trash)
      (e/drag-and-drop doc trash)
      (e/drag-and-drop doc trash)
      (-> (e/absent? doc) is))))

(deftest test-element-location
  (let [q             {:id :el-location-input}
        loc           (e/get-element-location *driver* q)
        {:keys [x y]} loc]
    (is (numeric? x))
    (is (numeric? y))))

;; Here and below: when running a Safari driver,
;; you need to unplug your second monitor. That sounds crazy,
;; I know. Bun nevertheless, if a Safari window appears on the second
;; monitor, the next two test will fail due to window error.

(deftest test-window-position
  (e/when-not-drivers
      [:phantom :edge] *driver*
      (let [{:keys [x y]} (e/get-window-position *driver*)]
        (is (numeric? x))
        (is (numeric? y))
        (e/set-window-position *driver* (+ x 10) (+ y 10))
        (let [{x' :x y' :y} (e/get-window-position *driver*)]
          (is (not= x x'))
          (is (not= y y'))))))

(deftest test-window-size
  (testing "getting size"
    (let [{:keys [width height]} (e/get-window-size *driver*)]
      (is (numeric? width))
      (is (numeric? height))
      (e/set-window-size *driver* (- width 10) (- height 10))
      (let [{width' :width height' :height} (e/get-window-size *driver*)]
        (is (not= width width'))
        (is (not= height height'))))))

(deftest test-switch-window
  (let [init-handle   (e/get-window-handle *driver*)
        init-url      (e/get-url *driver*)]
    ;; press enter on link instead of clicking (safaridriver is not great with the click)
    (e/fill *driver* :switch-window k/return)
    (e/when-safari *driver*
      (e/wait 3)) ;;safari seems to need a breather
    (is (= 2 (count (e/get-window-handles *driver*))) "2 windows now exist")
    (let [new-handles   (e/get-window-handles *driver*)
          new-handle    (first (filter #(not= % init-handle) new-handles))
          _             (e/switch-window *driver* new-handle)
          target-handle (e/get-window-handle *driver*)
          target-url    (e/get-url *driver*)]
      (is (not= init-handle target-handle))
      (is (= new-handle target-handle))
      (is (not= init-url target-url)))))

(deftest test-switch-window-next
  (let [init-handle (e/get-window-handle *driver*)]
    (doseq [_ (range 3)]
      ;; press enter on link instead of clicking (safaridriver is not great with click)
      (e/fill *driver* :switch-window k/return)
      ;; compensate: safari navigates to target window, others stay at source
      (e/when-safari *driver*
        (e/wait 3) ;; safari seems to need a breather
        (e/switch-window *driver* init-handle)))
    (is (= 4 (count (e/get-window-handles *driver*))) "4 windows now exist")
    (is (= init-handle (e/get-window-handle *driver*)) "on first window")
    (doseq [_ (range 3)]
      (e/switch-window-next *driver*)
      (is (not= init-handle (e/get-window-handle *driver*)) "navigating new windows"))
    (e/switch-window-next *driver*)
    (is (= init-handle (e/get-window-handle *driver*)) "wrapped around to original window")))

;; TODO: need refactoring not working for headless & firefox
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
    (e/when-not-safari *driver*
      (doto *driver*
        (e/click {:id :set-active-el})
        (-> (e/get-element-attr :active :id)
            (= "active-el-input")
            is)))))

(deftest test-element-text
  (let [text (e/get-element-text *driver* {:id :element-text})]
    (is (= text "Element text goes here."))))

(deftest test-element-size
  (let [{:keys [width height]} (e/get-element-size *driver* {:id :element-text})]
    (is (numeric? width))
    (is (numeric? height))))

(deftest test-cookies
  (testing "getting all cookies"
    (let [cookies (e/get-cookies *driver*)
          sorted-cookies (->> cookies
                              (map #(dissoc % :sameSite)) ;; varies, maybe we don't care about this one
                              (sort-by :name) ;; order varies we don't care
                              )]
      (is (= sorted-cookies [{:domain "localhost"
                              :httpOnly false
                              :name "cookie1"
                              :path "/"
                              :secure false
                              :value "test1"}
                             {:domain "localhost"
                              :httpOnly false
                              :name "cookie2"
                              :path "/"
                              :secure false
                              :value "test2"}]))))
  (testing "getting a cookie"
    (let [cookie (e/get-cookie *driver* :cookie2)
          cookie (dissoc cookie :sameSite)]
      (is (= cookie {:domain "localhost"
                     :httpOnly false
                     :name "cookie2"
                     :path "/"
                     :secure false
                     :value "test2"}))))
  (testing "deleting a cookie"
    (e/when-not-phantom
      *driver*
      (e/delete-cookie *driver* :cookie3)
      (let [cookie (e/get-cookie *driver* :cookie3)]
        (is (nil? cookie)))))
  (testing "deleting all cookies"
    (doto *driver*
      e/delete-cookies
      (-> e/get-cookies
          (= [])
          is))))

(deftest test-page-source
  (let [src (e/get-source *driver*)]
    (if (e/phantom? *driver*)
      (is (str/starts-with? src "<!DOCTYPE html>"))
      (is (str/starts-with? src "<html><head>")))))

(defn- valid-image? [file]
  (if-let [image-magick (some-> (fs/which (if (= "Linux" (os-name))
                                            "identify" ;; sacre ubuntu!
                                            "magick"))
                                str)]
    (let [{:keys [exit out]}
          (if (= "Linux" (os-name))
            (shell/sh image-magick (str file))
            (shell/sh image-magick "identify" (str file)))]
      (println out)
      (zero? exit))
    (throw (ex-info "please install image magick, we use it for screenshot image verification" {}))))

(deftest test-screenshot
  (util/with-tmp-file "screenshot" ".png" path
    (e/screenshot *driver* path)
    (is (valid-image? path))))

(deftest test-with-screenshots
  (fs/with-temp-dir [dir {:prefix "screenshots"}]
    (e/with-screenshots *driver* dir
      (e/fill *driver* :simple-input "1")
      (e/fill *driver* :simple-input "1")
      (e/fill *driver* :simple-input "1"))
    (is (= 3 (count (fs/list-dir dir))))))

(deftest test-screenshot-element
  (when (or (e/chrome? *driver*)
            (e/firefox? *driver*))
    (util/with-tmp-file "screenshot" ".png" path
      (e/screenshot-element *driver* {:id :css-test} path)
      (is (valid-image? path)))))

(deftest test-js-execute
  (testing "simple result"
    (let [result (e/js-execute *driver* "return 42;")]
      (is (= result 42))))
  (testing "with args"
    (let [script "return {foo: arguments[0], bar: arguments[1]};"
          result (e/js-execute *driver* script {:test 42} [true, nil, "Hello"])]
      (is (= result
             {:foo {:test 42}
              :bar [true nil "Hello"]})))))

(deftest test-add-script
  (let [js-url (test-server-url "js/inject.js")]
    (testing "adding a script"
      (e/add-script *driver* js-url)
      (e/wait 1)
      (let [result (e/js-execute *driver* "return injected_func();")]
        (is (= result "I was injected"))))))

(deftest test-set-hash
  (testing "set hash"
    (doto *driver*
      (e/set-hash "hello")
      (-> e/get-hash (= "hello") is)
      (-> e/get-url (str/ends-with? "/test.html#hello") is)
      (e/set-hash "goodbye")
      (-> e/get-url (str/ends-with? "/test.html#goodbye") is))))

(deftest test-find-element
  (let [text (e/get-element-text *driver* {:class :target})]
    (is (= text "target-1")))
  (let [text (e/get-element-text *driver* [{:class :foo}
                                         {:class :target}])]
    (is (= text "target-2")))
  (e/with-xpath *driver*
    (let [text (e/get-element-text *driver* ".//div[@class='target'][1]")]
      (is (= text "target-1"))))
  (let [text (e/get-element-text *driver* {:css ".target"})]
    (is (= text "target-1")))
  (let [q    [{:css ".bar"} ".//div[@class='inside']" {:tag :span}]
        text (e/get-element-text *driver* q)]
    (is (= text "target-3"))))

(deftest test-find-elements-more
  (testing "simple case"
    (let [q        {:class :find-elements-target}
          elements (e/query-all *driver* q)]
      (is (= (count elements) 4))))
  (testing "nested case"
    (let [q        [{:id :find-elements-nested}
                    {:class :nested}
                    {:class :target}]
          elements (e/query-all *driver* q)
          texts    (for [el elements]
                     (e/get-element-text-el *driver* el))]
      (is (= (count elements) 2))
      (is (= texts ["1" "2"])))))

(deftest test-multiple-elements
  (testing "tag names"
    (let [q         {:xpath ".//div[@id='operate-multiple-elements']//*"}
          elements  (e/query-all *driver* q)
          tag-names (for [el elements]
                      (str/lower-case (e/get-element-tag-el *driver* el)))]
      (is (= (vec tag-names)
             ["div" "b" "p" "span"])))))

(deftest test-query-tree
  (let [url            (test-server-url "test2.html")
        _              (e/go *driver* url)
        all-div        (e/query-tree *driver* {:tag :div})
        all-li         (e/query-tree *driver* {:tag :li})
        li-three-level (e/query-tree
                         *driver* {:tag :div} {:tag :div} {:tag :div} {:tag :li})
        tag-a          (e/query-tree *driver* {:tag :div} {:tag :div} {:tag :a})]
    (is (= 6 (count all-div)))
    (is (= 8 (count all-li)))
    (is (= 5 (count li-three-level)))
    (is (= 1 (count tag-a)))))

(deftest test-child
  (let [parent-el (e/query *driver* {:css "#wc3-barks"})
        child-el  (e/child *driver* parent-el {:css ".crypt-lord"})
        tag-name  (str/lower-case (e/get-element-tag-el *driver* child-el))
        tag-text  (e/get-element-text-el *driver* child-el)]
    (is (= "span" tag-name))
    (is (str/includes? tag-text "From the depths I've come!"))))

(deftest test-children
  (let [parent-el      (e/query *driver* {:css "#wc3-barks"})
        children-els   (e/children *driver* parent-el {:css "p"})
        children-texts (map #(e/get-element-text-el *driver* %) children-els)]
    (is (= ["p" "p"] (map #(str/lower-case (e/get-element-tag-el *driver* %)) children-els)))
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
        (e/with-postmortem *driver* {:dir dir-tmp}
          (e/click *driver* :non-existing-element))
        (is false "should be caught")
        (catch Exception _e
          (let [files               (file-seq (fs/file dir-tmp))
                expected-file-count (if (e/supports-logs? *driver*) 3 2)]
            (is (= (-> files rest count)
                   expected-file-count))))))))

(deftest test-find-quotes-in-text
  (doto *driver*
    (-> (e/has-text? "'quote") is)))

(deftest test-has-text
  (testing "test :fn/has-string"
    (is (boolean (e/query *driver* {:fn/has-string "From the depth"}))))
  (testing "gloval"
    (is (e/has-text? *driver* "From the depths I've come!"))
    (is (e/has-text? *driver* "I've come from the dark")))
  (testing "relative"
    (is (e/has-text? *driver* [:wc3-barks {:tag :p} {:tag :span}] "ths I've come!")))
  (testing "short path"
    (is (e/has-text? *driver* [:wc3-barks {:tag :span}] "ths I've")))
  (testing "wrong path"
    (is (not (e/has-text? *driver* [:wc3-barks {:tag :p} :pit-lord] "ths I've come!")))))


;; actions

(deftest test-actions
  (testing "input key and mouse click"
    (e/when-not-phantom *driver*
      (let [input    (e/query *driver* :simple-input)
            password (e/query *driver* :simple-password)
            textarea (e/query *driver* :simple-textarea)
            submit   (e/query *driver* :simple-submit)
            keyboard (-> (e/make-key-input)
                         e/add-double-pause
                         (e/with-key-down "\uE01B")
                         e/add-double-pause
                         (e/with-key-down "\uE01C")
                         e/add-double-pause
                         (e/with-key-down "\uE01D"))
            mouse    (-> (e/make-mouse-input)
                         (e/add-pointer-click-el input)
                         e/add-pause
                         (e/add-pointer-click-el password)
                         e/add-pause
                         (e/add-pointer-click-el textarea)
                         e/add-pause
                         (e/add-pointer-click-el submit))]
        (e/perform-actions *driver* keyboard mouse)
        (e/wait 1)
        (is (str/ends-with? (e/get-url *driver*) "?login=1&password=2&message=3"))))))

(deftest test-shadow-dom
  (testing "basic functional sanity"
    ;; Validate that the test DOM is as we would expect
    (is (e/has-text? *driver* {:id "not-in-shadow"} "I'm not in the shadow DOM"))
    (is (not (e/has-text? *driver* {:id "in-shadow"} "I'm in the shadow DOM"))))
  (testing "getting the shadow root for an element"
    (is (some? (e/get-element-shadow-root *driver* {:id "shadow-root-host"})))
    (is (some? (e/get-element-shadow-root-el *driver*
                                             (e/query *driver* {:id "shadow-root-host"})))))
  (testing "whether an element has a shadow root"
    (is (e/has-shadow-root? *driver* {:id "shadow-root-host"}))
    (is (e/has-shadow-root-el? *driver* (e/query *driver* {:id "shadow-root-host"}))))
  (let [shadow-root (e/get-element-shadow-root *driver* {:id "shadow-root-host"})]
    (testing "querying the shadow root element for a single element"
      (is (= "I'm in the shadow DOM"
             (->> (e/query-shadow-root-el *driver*
                                          shadow-root
                                          {:css "#in-shadow"})
                  (e/get-element-text-el *driver*))))
      (is (= "I'm also in the shadow DOM"
             (->> (e/query-shadow-root-el *driver*
                                          shadow-root
                                          {:css "#also-in-shadow"})
                  (e/get-element-text-el *driver*)))))
    (testing "querying the shadow root element for multiple elements"
      (is (= ["I'm in the shadow DOM" "I'm also in the shadow DOM"]
             (->> (e/query-all-shadow-root-el *driver*
                                              shadow-root
                                              {:css "span"})
                  (mapv #(e/get-element-text-el *driver* %)))))))
  (testing "querying the shadow root element"
    (is (= "I'm in the shadow DOM"
           (->> (e/query-shadow-root *driver* {:id "shadow-root-host"} {:css "#in-shadow"})
                (e/get-element-text-el *driver*)))))
  (testing "querying the shadow root element for multiple elements"
    (is (= ["I'm in the shadow DOM" "I'm also in the shadow DOM"]
           (->> (e/query-all-shadow-root *driver* {:id "shadow-root-host"} {:css "span"})
                (mapv #(e/get-element-text-el *driver* %)))))))

(comment
  ;; start test server
  (def test-server (p/process {:out :inherit :err :inherit} "bb test-server --port" 9993))
  (def url (format "http://localhost:%d/%s" 9993 "test.html"))

  ;; start your favourite webdriver
  (def driver (e/safari))
  (def driver (e/firefox))

  ;; mimic test fixture
  (e/go driver url)
  (e/wait-visible driver {:id :document-end})

  ;; cleanup
  (e/quit driver)
  (p/destroy test-server)

  :eoc)
