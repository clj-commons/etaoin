(ns etaoin.api-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [cheshire.core :as json]
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
   [slingshot.slingshot :refer [try+]]
   [slingshot.test])
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
             ;; add logging for kind of flaky CI scenario (maybe we'll answer why we need
             ;; to retry launching safaridriver automatically)
             (ci?) (merge {:log-stdout :inherit
                           :log-stderr :inherit
                           :driver-log-level "debug"
                           :post-stop-fns [(fn dump-discovered-log [driver]
                                             (if-let [log (:driver-log-file driver)]
                                               (do
                                                 (println "-[start]-safaridriver log file" log)
                                                 (with-open [in (io/input-stream log)]
                                                   (io/copy in *out*))
                                                 (println "-[end]-safaridriver log file" log))
                                               (println "-no safaridriver log file discovered-")))]}))
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
          (f))))))

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
          (let [resp (try (client/http-request {:method :get :uri test-url})
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

(defn reload-test-page
  []
  (e/go *driver* (test-server-url "test.html"))
  (e/wait-visible *driver* {:id :document-end}))

(defmacro wait-url-change
  [re & body]
  `(let [old-url# (e/get-url *driver*)]
     ~@body
     (e/wait-predicate (fn [] (let [new-url# (e/get-url *driver*)]
                                (and (not= old-url# new-url#)
                                     (re-find ~re new-url#))))
                       {:timeout 30     ; 30 second timeout total
                        :interval 0.100 ; poll at 100 msec interval
                        :message "Timeout waiting for URL change"})))

(deftest test-browser-conditionals
  (testing "Chrome conditionals"
    (e/when-chrome *driver*
                   (is (e/driver? *driver* :chrome)))
    (e/when-not-chrome *driver*
                       (is (not (e/driver? *driver* :chrome)))))
  (testing "Firefox conditionals"
    (e/when-firefox *driver*
                    (is (e/driver? *driver* :firefox)))
    (e/when-not-firefox *driver*
                        (is (not (e/driver? *driver* :firefox)))))
  (testing "Safari conditionals"
    (e/when-safari *driver*
                   (is (e/driver? *driver* :safari)))
    (e/when-not-safari *driver*
                       (is (not (e/driver? *driver* :safari)))))
  (testing "Edge conditionals"
    (e/when-edge *driver*
                 (is (e/driver? *driver* :edge)))
    (e/when-not-edge *driver*
                     (is (not (e/driver? *driver* :edge)))))
  (testing "Headless conditionals"
    (e/when-headless *driver*
                     (is (e/headless? *driver*)))
    (e/when-not-headless *driver*
                         (is (not (e/headless? *driver*))))))

(deftest test-navigation
  (is (= (test-server-url "test.html")  (e/get-url *driver*)) "initial page")
  (e/go *driver* (test-server-url "test2.html"))
  (is (= (test-server-url "test2.html")  (e/get-url *driver*)) "navigate to 2nd page")
  (e/back *driver*)
  (is (= (test-server-url "test.html")  (e/get-url *driver*)) "back to initial page")
  (e/forward *driver*)
  (is (= (test-server-url "test2.html")  (e/get-url *driver*)) "forward to 2nd page"))

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

(deftest test-submit
  (e/fill-multi *driver* {:simple-input    1
                          :simple-password 2
                          :simple-textarea 3})
  (wait-url-change #"login"
   (e/submit *driver* :simple-input))
  (is (str/ends-with? (e/get-url *driver*) "?login=1&password=2&message=3")))

(deftest test-input
  (testing "fill multiple inputs"
    ;; Test with map form
    (e/fill-multi *driver* {:simple-input    1
                            :simple-password 2
                            :simple-textarea 3})
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*) "?login=1&password=2&message=3"))
    ;; Test with vector form
    (e/fill-multi *driver*
                  [:simple-input    4
                   :simple-password 5
                   :simple-textarea 6])
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*) "?login=4&password=5&message=6")))
  (testing "fill-multi bad inputs"
    (is (thrown+? [:type :etaoin/argument]
                  (e/fill-multi *driver* #{:set :is :not :allowed})))
    (is (thrown+? [:type :etaoin/argument]
                  (e/fill-multi *driver* '(:list :is :not :allowed))))
    (is (thrown+? [:type :etaoin/argument]
                  (e/fill-multi *driver* [:vector :with :odd :length :is :not :allowed]))))
  (testing "fill human multiple inputs"
    ;; Test with map form
    (e/fill-human-multi *driver*
                        {:simple-input    "login"
                         :simple-password "123"
                         :simple-textarea "text"})
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*) "?login=login&password=123&message=text"))
    ;; Test with vector form
    (e/fill-human-multi *driver*
                        [:simple-input    "login2"
                         :simple-password "456"
                         :simple-textarea "text2"])
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*) "?login=login2&password=456&message=text2")))
  (testing "fill-human-multi bad inputs"
    (is (thrown+? [:type :etaoin/argument]
                  (e/fill-multi *driver* #{:set :is :not :allowed})))
    (is (thrown+? [:type :etaoin/argument]
                  (e/fill-multi *driver* '(:list :is :not :allowed))))
    (is (thrown+? [:type :etaoin/argument]
                  (e/fill-multi *driver* [:vector :with :odd :length :is :not :allowed]))))
  (testing "fill multiple vars"
    (e/fill *driver* :simple-input 1 "test" 2 \space \A)
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*) "?login=1test2+A&password=&message=")))
  (testing "fill active"
    (e/click *driver* :simple-input)
    (e/fill-active *driver* "MyLogin")
    (e/click *driver* :simple-password)
    (e/fill-active *driver* "MyPassword")
    (e/click *driver* :simple-textarea)
    (e/fill-active *driver* "Some text")
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*)
                        "?login=MyLogin&password=MyPassword&message=Some+text")))
  (testing "fill active human"
    (e/click *driver* :simple-input)
    (e/fill-human-active *driver* "MyLogin2")
    (e/click *driver* :simple-password)
    (e/fill-human-active *driver* "MyPassword2")
    (e/click *driver* :simple-textarea)
    (e/fill-human-active *driver* "Some text 2")
    (wait-url-change #"login" (e/click *driver* :simple-submit))
    (is (str/ends-with? (e/get-url *driver*)
                        "?login=MyLogin2&password=MyPassword2&message=Some+text+2"))))

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
  (e/when-not-drivers #{:chrome :edge} *driver*
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
    (e/fill *driver* {:id :simple-input} "test")
    (e/clear *driver* {:id :simple-input})
    (wait-url-change #"login" (e/click *driver* {:id :simple-submit}))
    (is (str/ends-with? (e/get-url *driver*) "?login=&password=&message=")))

  (testing "multiple clear"
    ;; Note that we need to reload the test page here because the URL
    ;; from the first test is the same as the second and thus if we
    ;; didn't do this we couldn't detect whether anything happened
    ;; after the first test.
    (reload-test-page)
    (e/fill-multi *driver*
                  {:simple-input    1
                   :simple-password 2
                   :simple-textarea 3})
    (e/clear *driver*
             :simple-input
             :simple-password
             :simple-textarea)
    (wait-url-change #"login" (e/click *driver* {:id :simple-submit}))
    (is (str/ends-with? (e/get-url *driver*) "?login=&password=&message="))))

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
    (-> e/has-alert? not is)))

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
  (is (= (e/get-title *driver*) "Webdriver Test Document")))

(deftest test-url
  (is (str/ends-with? (e/get-url *driver*) "/test.html")))

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

(deftest test-drag-and-drop
  (let [url   (test-server-url "drag-n-drop/index.html")
        doc   {:class :document}
        trash {:xpath "//div[contains(@class, 'trash')]"}]
    (e/go *driver* url)
    (is (= 4 (count (e/query-all *driver* doc)))
        "doc count at start")

    (doseq [n (range 1 5)]
      (e/drag-and-drop *driver* doc trash)
      (is (= (- 4 n) (count (e/query-all *driver* doc)))
          (format "doc count after drag and drop # %d" n)))

    (is (e/absent? *driver* doc)
        "no docs after last drag and drop")))

(deftest test-drag-and-drop-alt
  (let [url   (test-server-url "drag-n-drop/index.html")
        doc   {:class :document}
        trash {:xpath "//div[contains(@class, 'trash')]"}]
    (e/go *driver* url)
    (is (= 4 (count (e/query-all *driver* doc)))
        "doc count at start")

    (e/perform-actions *driver*
                       (-> (e/make-mouse-input)
                           (e/add-pointer-move-to-el (e/query *driver* doc))
                           (e/with-pointer-left-btn-down
                             (e/add-pointer-move-to-el (e/query *driver* trash)))))
    (is (= 3 (count (e/query-all *driver* doc)))
        "doc count at end")))

(deftest test-double-click
  ;; Does not work on Safari 2024-08-10
  (e/when-not-safari *driver*
    (e/scroll-bottom *driver*) ;; element needs to be in viewport
                               ;; TODO: look at wheel action
    (is (= "[not clicked]" (e/get-element-text *driver* :pointerClickTarget)))
    (e/double-click *driver* :pointerClickTarget)
    (is (= "[double clicked]" (e/get-element-text *driver* :pointerClickTarget)))))

(deftest test-element-location
  (let [q             {:id :el-location-input}
        loc           (e/get-element-location *driver* q)
        {:keys [x y]} loc]
    (is (numeric? x))
    (is (numeric? y))))

;; Still relevant?:
;; Here and below: when running a Safari driver,
;; you need to unplug your second monitor. That sounds crazy,
;; I know. Bun nevertheless, if a Safari window appears on the second
;; monitor, the next two test will fail due to window error.

(deftest test-window-position
  (e/when-not-drivers
    [:edge] ;; edge fails this test
    *driver*
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

(deftest test-maximize
  (e/when-not-headless *driver* ;; skip for headless
    (e/set-window-position *driver* 2 2)
    (let [orig-rect (e/get-window-rect *driver*)
          target-rect (-> orig-rect
                          (update :x #(+ % 2))
                          (update :y #(+ % 2))
                          (update :width #(- % 5))
                          (update :height #(- % 5)))]
      ;; move the window to ensure values will change when maximized
      (e/set-window-rect *driver* target-rect)
      (let [moved-rect (e/get-window-rect *driver*)]
        ;; sanity test for move
        (is (= target-rect moved-rect))
        (e/maximize *driver*)
        (let [maximed-rect (e/get-window-rect *driver*)]
          (is (not= moved-rect maximed-rect)))))))

(deftest test-active-element
  (e/click *driver* {:id :set-active-el})
  ;; Test old query API since get-element-attr uses query underneath
  (is (= "active-el-input" (e/get-element-attr *driver* :active :id)))
  ;; Test old query using :active directly
  (is (= "active-el-input" (e/get-element-attr-el *driver*
                                                  (e/query *driver* :active)
                                                  :id)))
  ;; Test new get-active-element API. This is preferred.
  (is (= "active-el-input" (e/get-element-attr-el *driver*
                                                  (e/get-active-element *driver*)
                                                  :id))))

(deftest test-element-text
  (let [text (e/get-element-text *driver* {:id :element-text})]
    (is (= text "Element text goes here."))))

(deftest test-element-size
  (let [{:keys [width height]} (e/get-element-size *driver* {:id :element-text})]
    (is (numeric? width))
    (is (numeric? height))))

(deftest get-element-tag
  (is (= "TITLE" (str/upper-case (e/get-element-tag *driver* {:tag :title})))))

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
  (testing "setting a cookie"
    (let [cookie {:name "etaoin-testing123" :value "foobarbaz"}]
      (e/set-cookie *driver* cookie)
      (is (= cookie
             (select-keys (e/get-cookie *driver* :etaoin-testing123) [:name :value])))))
  (testing "deleting a cookie"
    (e/delete-cookie *driver* :cookie3)
    (let [cookie (e/get-cookie *driver* :cookie3)]
      (is (nil? cookie))))
  (testing "deleting all cookies"
    (doto *driver*
      e/delete-cookies
      (-> e/get-cookies
          (= [])
          is))))

(deftest test-page-source
  (let [src (e/get-source *driver*)]
    (is (str/starts-with? src "<html><head>"))))

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
  (util/with-tmp-file "screenshot" ".png" path
    (e/screenshot-element *driver* {:id :css-test} path)
    (is (valid-image? path))))

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
    (e/set-hash *driver* "hello")
    (is (= (e/get-hash *driver*) "hello"))
    (is (str/ends-with? (e/get-url *driver*) "/test.html#hello"))
    (e/set-hash *driver* "goodbye")
    (is (str/ends-with? (e/get-url *driver*) "/test.html#goodbye"))))

(deftest test-query
  (testing "finding an element by id keyword"
    (let [el (e/query *driver* :find-element-by-id)]
      (is (= "target-1" (e/get-element-text-el *driver* el)))))
  (testing "XPath and CSS string syntax"
    (e/with-xpath *driver*
      (let [el (e/query *driver* ".//div[@class='target'][1]")]
        (is (= "target-1" (e/get-element-text-el *driver* el)))))
    (e/with-css *driver*
      (let [el (e/query *driver* ".bar .deep .inside span") ]
        (is (= "target-3" (e/get-element-text-el *driver* el))))))
  (testing "XPath and CSS map syntax"
    (let [el (e/query *driver* {:xpath ".//*[@class='target']"})]
      (is (= "target-1" (e/get-element-text-el *driver* el))))
    (let [el (e/query *driver* {:css ".target"})]
      (is (= "target-1" (e/get-element-text-el *driver* el)))))
  (testing "map syntax"
    ;; 1. tags
    (testing "tags"
      (let [el (e/query *driver* {:tag :h3 :id :find-element})]
        (is (= "Find element" (e/get-element-text-el *driver* el)))))
    ;; 2. class
    (testing "class"
      (let [el (e/query *driver* {:class :target})]
        (is (= "target-1" (e/get-element-text-el *driver* el)))))
    ;; 3. random attributes
    (testing "random attributes"
      (let [el (e/query *driver* {:strangeattribute :foo})]
        (is (= "DIV with strange attribute" (e/get-element-text-el *driver* el)))))
    ;; 4. :fn/*
    (testing ":fn/* functions"
      ;; :index and :fn/index
      (let [el (e/query *driver* {:class :list :index 3})] ; deprecated syntax
        (is (= "ordered 3" (e/get-element-text-el *driver* el))))
      (let [el (e/query *driver* {:class :list :fn/index 3})] ; new syntax
        (is (= "ordered 3" (e/get-element-text-el *driver* el))))
      ;; :fn/text
      (let [el (e/query *driver* {:fn/text "multiple classes"})]
        (is (= "multiple-classes" (e/get-element-attr-el *driver* el "id"))))
      ;; :fn/has-text
      (let [el (e/query *driver* {:fn/has-text "ple cla"})] ; pick out the middle
        (is (= "multiple-classes" (e/get-element-attr-el *driver* el "id"))))
      ;; :fn/has-string
      (let [el (e/query *driver* {:tag :ol :fn/has-string "ordered 3"})]
        (is (= "ordered-list"  (e/get-element-attr-el *driver* el "id"))))
      ;; :fn/has-class
      (let [el (e/query *driver* {:fn/has-class "ol-class1"})]
        (is (= "ordered-list"  (e/get-element-attr-el *driver* el "id"))))
      ;; :fn/has-classes
      ;; verify that order doesn't matter
      (let [elx (e/query *driver* {:fn/has-classes [:ol-class1 :ol-class2]})
            ely (e/query *driver* {:fn/has-classes [:ol-class2 :ol-class1]})
            elz (e/query *driver* :ordered-list)]
        (is (= elx ely elz)))
      ;; :fn/link
      (let [el (e/query *driver* {:fn/link "https://www.github.com/"})]
        (is (= "Link to GitHub" (e/get-element-text-el *driver* el))))
      ;; :fn/enabled
      (let [el (e/query *driver* [:enabled-disabled {:type :checkbox :fn/enabled true}])]
        (is (= "checkbox-1" (e/get-element-attr-el *driver* el "id"))))
      (let [el (e/query *driver* [:enabled-disabled {:type :checkbox :fn/enabled false}])]
        (is (= "checkbox-2" (e/get-element-attr-el *driver* el "id"))))
      (let [el (e/query *driver* [:enabled-disabled {:type :checkbox :fn/enabled true :fn/index 2}])]
        (is (= "checkbox-3" (e/get-element-attr-el *driver* el "id"))))
      ;; :fn/disabled
      (let [el (e/query *driver* [:enabled-disabled {:type :checkbox :fn/disabled false}])]
        (is (= "checkbox-1" (e/get-element-attr-el *driver* el "id"))))
      (let [el (e/query *driver* [:enabled-disabled {:type :checkbox :fn/disabled true}])]
        (is (= "checkbox-2" (e/get-element-attr-el *driver* el "id"))))
      (let [el (e/query *driver* [:enabled-disabled {:type :checkbox :fn/disabled false :fn/index 2}])]
        (is (= "checkbox-3" (e/get-element-attr-el *driver* el "id"))))))
  (testing "vector syntax"
    ;; TODO: should check vectors with length 1, 2, and 3.
    (e/with-xpath *driver*       ; force XPath because we use a string
      (let [el (e/query *driver* [{:css ".bar"} ".//div[@class='inside']" {:tag :span}])]
        (is (= "target-3" (e/get-element-text-el *driver* el)))))
    (let [el (e/query *driver* [{:class :foo} {:class :target}])]
      (is (= "target-2" (e/get-element-text-el *driver* el)))))
  (testing "variable arguments syntax"
    ;; Same as vector syntax but just provided as separate arguments to `query`
    (e/with-xpath *driver*
      (let [el (e/query *driver* {:css ".bar"} ".//div[@class='inside']" {:tag :span})]
        (is (= "target-3" (e/get-element-text-el *driver* el)))))
    (let [el (e/query *driver* {:class :foo} {:class :target})]
      (is (= "target-2" (e/get-element-text-el *driver* el)))))
  (testing "negative test cases"
    ;; TODO:
    ;; 1. searching for nothing
    (testing "zero-length vector queries"
      ;; 1. pass a vector of length 0 to query
      (is (thrown+? [:type :etaoin/argument] (e/query *driver* []))))
    ;; 2. searching for an element that can't be found
    (testing "querying for missing elements"
      ;; 2a. searching for a missing element with missing ID
      (is (thrown+? [:type :etaoin/http-error] (e/query *driver* :missing-element)))
      (is (thrown+? [:type :etaoin/http-error] (e/query *driver* [:missing-element])))
      ;; 2a. element not found in middle of a vector query
      (is (thrown+? [:type :etaoin/http-error] (e/query *driver* [{:css ".bar"}
                                                                  :missing-element
                                                                  {:tag :span}])))
      ;; 2b. element not found at the end of a vector query
      (is (thrown+? [:type :etaoin/http-error] (e/query *driver* [{:css ".bar"}
                                                                  {:tag :div :class :inside}
                                                                  :missing-element]))))
    ;; 3. malformed XPath
    ;; 4. malformed CSS
    ;; 5. query isn't a string, map, or vector. Perhaps a list and set.
    ;; 6. unknown :fn/... keywords
    (testing "unknown :fn/* keywords"
      ;; ":fn/indx" is probably a typo and the user really wants ":fn/index"
      (is (thrown+? [:type :etaoin/argument] (e/query *driver* {:tag :div :fn/indx 1}))))
    ;; 7. vector queries with vector elements (vectors in vectors)
    ))

(deftest test-query-all
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
      (is (= texts ["1" "2"]))))
  (testing "returning multiple elements via XPath"
    (let [q         {:xpath ".//div[@id='operate-multiple-elements']//*"}
          elements  (e/query-all *driver* q)
          tag-names (for [el elements]
                      (str/lower-case (e/get-element-tag-el *driver* el)))]
      (is (= (vec tag-names)
             ["div" "b" "p" "span"])))))

(deftest test-switch-default-locator
  (testing "xpath locator"
    (let [driver (e/use-xpath *driver*)]
      (is (= "target-1" (e/get-element-text driver ".//*[@class='target']")))))
  (testing "css locator"
    (let [driver (e/use-css *driver*)]
      (is (= "target-1" (e/get-element-text driver ".target"))))))

(deftest test-fn-index
  (testing ":fn/index"
    (let [items (for [index (range 1 6)]
                  (->> (e/query *driver* {:class :indexed :fn/index index})
                       (e/get-element-text-el *driver*)))]
      (is (= items ["One" "Two" "Three" "Four" "Five"])))))

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

(deftest test-query-from
  (testing "single item query"
    (let [parent-el (e/query *driver* {:css "#wc3-barks"})
          child-el  (e/query-from *driver* parent-el {:css ".crypt-lord"})
          tag-name  (str/lower-case (e/get-element-tag-el *driver* child-el))
          tag-text  (e/get-element-text-el *driver* child-el)]
      (is (= "span" tag-name))
      (is (str/includes? tag-text "From the depths I've come!"))))
  (testing "vector query"
    (let [start-el (e/query *driver* :deep-query-root)
          final-el (e/query-from *driver*
                                 start-el
                                 [{:class "intermediate-node-1-2"}
                                  ;; skip over intermediate-node-2
                                  {:css "#intermediate-node-3"}
                                  :intermediate-node-4
                                  {:tag "li"}])
          tag-name (str/lower-case (e/get-element-tag-el *driver* final-el))
          tag-text (e/get-element-text-el *driver* final-el)]
      (is (= "li" tag-name))
      (is (= "One" tag-text)))))

(deftest test-query-all-from
  (testing "single item query"
    (let [parent-el      (e/query *driver* {:css "#wc3-barks"})
          children-els   (e/query-all-from *driver* parent-el {:css "p"})
          children-texts (map #(e/get-element-text-el *driver* %) children-els)]
      (is (= ["p" "p"] (map #(str/lower-case (e/get-element-tag-el *driver* %)) children-els)))
      (is (str/includes? (first children-texts) "From the depths I've come!"))
      (is (str/includes? (last children-texts) "I've come from the darkness of the pit!"))))
  (testing "vector query"
    (let [start-el (e/query *driver* :deep-query-root)
          final-els (e/query-all-from *driver*
                                      start-el
                                      [{:class "intermediate-node-1-2"}
                                       ;; skip over intermediate-node-2
                                       {:css "#intermediate-node-3"}
                                       :intermediate-node-4
                                       {:tag "li"}])
          tag-names (mapv #(str/lower-case (e/get-element-tag-el *driver* %)) final-els)
          tag-texts (mapv #(e/get-element-text-el *driver* %) final-els)]
      (is (= ["li" "li" "li"] tag-names))
      (is (= ["One" "Two" "Three"] tag-texts)))))

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

(deftest test-mouse-state-actions
  (testing "mouse state and release"
    (is (= "[no clicks yet]" (e/get-element-text *driver* :mouseButtonState)))
    (testing "left mouse down"
      (e/perform-actions *driver* (-> (e/make-mouse-input)
                                      (e/add-pointer-down)))
      (let [button-state (-> (e/get-element-text *driver* :mouseButtonState)
                             (json/parse-string true))]
        (is (= {:type "mousedown"
                :left true
                :right false
                :wheel false
                :back false
                :forward false} button-state))))
    (e/when-not-safari *driver* ;; safari currently fails behaves differently one 2024-08-10
      (testing "right mouse down is not additive to left mouse down in new transaction"
        (e/perform-actions *driver* (-> (e/make-mouse-input)
                                        (e/add-pointer-down k/mouse-middle)))
        (let [button-state (-> (e/get-element-text *driver* :mouseButtonState)
                               (json/parse-string true))]
          (is (= {:type "mousedown"
                  :left false
                  :right false
                  :wheel true
                  :back false
                  :forward false} button-state))))
      (testing "multiple mouse buttons can be pressed in a single transaction"
        (e/perform-actions *driver* (-> (e/make-mouse-input)
                                        (e/add-pointer-down k/mouse-left)
                                        (e/add-pointer-down k/mouse-middle)))
        (let [button-state (-> (e/get-element-text *driver* :mouseButtonState)
                               (json/parse-string true))]
          (is (= {:type "mousedown"
                  :left true
                  :right false
                  :wheel true
                  :back false
                  :forward false} button-state)))))
    (testing "release actions wipes state"
      (e/release-actions *driver*)
      (let [button-state (-> (e/get-element-text *driver* :mouseButtonState)
                             (json/parse-string true))]
          (is (= {:type "mouseup"
                  :left false
                  :right false
                  :wheel false
                  :back false
                  :forward false} button-state))))))

(deftest test-combined-actions
    (testing "input key and mouse click"
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
          (wait-url-change #"login"
           (e/perform-actions *driver* keyboard mouse))
          (is (str/ends-with? (e/get-url *driver*) "?login=1&password=2&message=3")))))

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
    (is (e/has-shadow-root-el? *driver* (e/query *driver* {:id "shadow-root-host"})))
    (is (not (e/has-shadow-root? *driver* :not-in-shadow)))
    (is (not (e/has-shadow-root-el? *driver* (e/query *driver* :not-in-shadow)))))
  (let [shadow-root (e/get-element-shadow-root *driver* {:id "shadow-root-host"})]
    (testing "querying the shadow root element for a single element"
      (is (= "I'm in the shadow DOM"
             (->> (e/query-from-shadow-root-el *driver*
                                               shadow-root
                                               {:css "#in-shadow"})
                  (e/get-element-text-el *driver*))))
      (is (= "I'm also in the shadow DOM"
             (->> (e/query-from-shadow-root-el *driver*
                                               shadow-root
                                               {:css "#also-in-shadow"})
                  (e/get-element-text-el *driver*)))))
    (testing "vector syntax for single element shadow root queries"
      (is (thrown? Exception
                   (->> (e/query-from-shadow-root-el *driver*
                                                     shadow-root
                                                     [])
                        (e/get-element-text-el *driver*))))
      (is (= "Level 3 text."
             (->> (e/query-from-shadow-root-el *driver*
                                               shadow-root
                                               [{:css "#level-3"}])
                  (e/get-element-text-el *driver*)
                  str/trim)))
      (is (= "Level 3 text."
             (->> (e/query-from-shadow-root-el *driver*
                                               shadow-root
                                               [{:css "#level-2"}
                                                {:css "#level-3"}])
                  (e/get-element-text-el *driver*)
                  str/trim)))
      (is (= "Level 3 text."
             (->> (e/query-from-shadow-root-el *driver*
                                               shadow-root
                                               [{:css "#level-1"}
                                                {:css "#level-2"}
                                                {:css "#level-3"}])
                  (e/get-element-text-el *driver*)
                  str/trim))))
    (testing "querying the shadow root element for multiple elements"
      (is (= ["I'm in the shadow DOM" "I'm also in the shadow DOM" "1" "2" "3"]
             (->> (e/query-all-from-shadow-root-el *driver*
                                                   shadow-root
                                                   {:css "span"})
                  (mapv #(e/get-element-text-el *driver* %))))))
    (testing "vector syntax for -all shadow root queries"
      (is (thrown? Exception
                   (e/query-all-from-shadow-root-el *driver*
                                                    shadow-root
                                                    [])))
      (is (= ["I'm in the shadow DOM" "I'm also in the shadow DOM" "1" "2" "3"]
             (->> (e/query-all-from-shadow-root-el *driver*
                                                   shadow-root
                                                   [{:css "span"}])
                  (map #(e/get-element-text-el *driver* %)))))
      (is (= ["1" "2" "3"]
             (->> (e/query-all-from-shadow-root-el *driver*
                                                   shadow-root
                                                   [{:css "#level-3-all"}
                                                    {:css "span"}])
                  (map #(e/get-element-text-el *driver* %)))))
      (is (= ["1" "2" "3"]
             (->> (e/query-all-from-shadow-root-el *driver*
                                                   shadow-root
                                                   [{:css "#level-2"}
                                                    {:css "#level-3-all"}
                                                    {:css "span"}])
                  (map #(e/get-element-text-el *driver* %)))))
      (is (= ["1" "2" "3"]
             (->> (e/query-all-from-shadow-root-el *driver*
                                                   shadow-root
                                                   [{:css "#level-1"}
                                                    {:css "#level-2"}
                                                    {:css "#level-3-all"}
                                                    {:css "span"}])
                  (map #(e/get-element-text-el *driver* %)))))))
  (testing "querying the shadow root element"
    (is (= "I'm in the shadow DOM"
           (->> (e/query-from-shadow-root *driver* {:id "shadow-root-host"} {:css "#in-shadow"})
                (e/get-element-text-el *driver*)))))
  (testing "querying the shadow root element for multiple elements"
    (is (= ["I'm in the shadow DOM" "I'm also in the shadow DOM" "1" "2" "3"]
           (->> (e/query-all-from-shadow-root *driver* {:id "shadow-root-host"} {:css "span"})
                (mapv #(e/get-element-text-el *driver* %)))))))

(deftest test-driver-type
  (is (#{:chrome :firefox :safari :edge} (e/driver-type *driver*))))

(deftest test-intersects
  (testing "self-intersection"
    ;; Things must intersect with themselves
    (is (e/intersects? *driver* {:class :target} {:class :target})))
  (testing "non-intersection"
    ;; Choose two things near each other, that overlap in one
    ;; dimension, but not two
    (is (not (e/intersects? *driver*
                            {:class :target}
                            [{:class :bar} {:class :deep}
                             {:class :inside} {:tag :span}])))))

(deftest test-timeouts
  (testing "basic timeout tests"
    (let [timeouts {:implicit 32134
                    :script 78921
                    :pageLoad 98765}]
      (e/set-timeouts *driver* timeouts)
      (is (= timeouts (e/get-timeouts *driver*)))
      (e/set-page-load-timeout *driver* 987)
      (e/set-implicit-timeout *driver* 876)
      (e/set-script-timeout *driver* 765)
      (is (= 987 (e/get-page-load-timeout *driver*)))
      (is (= 876 (e/get-implicit-timeout *driver*)))
      (is (= 765 (e/get-script-timeout *driver*)))
      (is (= {:pageLoad 987000 :implicit 876000 :script 765000}
             (e/get-timeouts *driver*)))))
  (testing "non-integer timeout values"
    ;; These should not throw. If they do, the test runner will record
    ;; a test failure.
    (is (do (e/set-page-load-timeout *driver* 1.33333)
            true))
    (is (do (e/set-page-load-timeout *driver* 100/3)
            true))))

(comment
  ;; start test server
  (def test-server (p/process {:out :inherit :err :inherit} "bb test-server --port" 9993))
  (def url (format "http://localhost:%d/%s" 9993 "test.html"))

  ;; start your favourite webdriver
  (def driver (e/chrome))
  (def driver (e/safari))
  (def driver (e/firefox))

  ;; mimic test fixture
  (e/go driver url)
  (e/wait-visible driver {:id :document-end})

  ;; cleanup
  (e/quit driver)
  (p/destroy test-server)

  :eoc)
