(ns webdriver.test
  (:require [clojure.string :as str]
            [webdriver.proc :as proc]
            [webdriver.client :as client]
            [clojure.java.io :as io]
            [clojure.test :refer [is
                                  deftest
                                  use-fixtures
                                  testing]]
            [cheshire.core :refer [parse-string]]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import java.net.ConnectException))

(def ^:dynamic *server*)
(def ^:dynamic *session*)
(def ^:dynamic *locator* "xpath")

(defmacro with-locator [locator & body]
  `(binding [*locator* ~locator]
     ~@body))

(defmacro with-http [meth path data bind & body]
  `(let [~bind (client/call *server* ~meth ~path ~data)]
     ~@body))

(defmacro with-http-get [path bind & body]
  `(with-http :get ~path nil ~bind
     ~@body))

(defmacro with-http-post [path body bind & body]
  `(with-http :post ~path ~body ~bind
     ~@body))

(defn- browser-dispatch [& _]
  (:browser *server*))

(defmacro defmethods [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))

(defmulti get-el browser-dispatch)

(defmethod get-el :firefox [q]
  (with-http
    :post
    [:session *session* :element]
    {:using *locator* :value q}
    resp
    (-> resp first second)))

(defmethods get-el [:chrome :phantom] [q]
  (with-http
    :post
    [:session *session* :element]
    {:using *locator* :value q}
    resp
    (-> resp :ELEMENT)))

(defmacro with-el [q bind & body]
  `(let [~bind (get-el ~q)]
     ~@body))

(defmulti get-window-handle browser-dispatch)

(defmethod get-window-handle :firefox []
  (with-http-get [:session *session* :window] resp
    (-> resp :value)))

(defmethod get-window-handle :default []
  (with-http-get [:session *session* :window_handle] resp
    (:value resp)))

(defmacro with-window-handle [bind & body]
  `(let [~bind (get-window-handle)]
     ~@body))

;; api

(defmacro with-css-selector [& body]
  `(with-locator "css selector"
     ~@body))

(defmacro with-link-text [& body]
  `(with-locator "link text"
     ~@body))

(defmacro with-partial-link-text [& body]
  `(with-locator "partial link text"
     ~@body))

(defmacro with-tag-name [& body]
  `(with-locator "tag name"
     ~@body))

(defmacro with-xpath [& body]
  `(with-locator "xpath"
     ~@body))

(defn go [url]
  (with-http :post [:session *session* :url] {:url url} _))

(defn click [q]
  (with-el q el
    (with-http-post [:session *session* :element el :click] nil _)))

(defn tag [q]
  (with-el q el
    (with-http-get
      [:session *session* :element el :name]
      resp
      (:value resp))))

(defn new-session []
  (with-http :post [:session]
    {:desiredCapabilities {} :requiredCapabilities {}}
    resp
    (:sessionId resp)))

(defn delete-session []
  (with-http :delete [:session *session*] nil _))

(defn make-server-url [host port]
  (format "http://%s:%d" host port))

(defn make-server [{:keys [host port] :as params}]
  (assoc params :url (make-server-url host port)))

(defmacro with-proc [proc args & body]
  `(let [~proc (apply proc/run ~args)]
     (try
       ~@body
       (finally
         (proc/kill ~proc)))))

(defmacro with-server [params & body]
  `(binding [*server* (make-server ~params)]
     ~@body))

(defmacro with-session [& body]
  `(binding [*session* (new-session)]
     (try
       ~@body
       (finally
         (delete-session)))))

(defmulti fullscreen browser-dispatch)

(defmethod fullscreen :firefox []
  (with-http-post
    [:session *session* :window :fullscreen] nil _))

(defmulti maximize browser-dispatch)

(defmethod maximize :firefox []
  (with-http :post
    [:session *session* :window :maximize] {} _))

(defmethod maximize :chrome []
  (with-window-handle h
    (with-http :post [:session *session* :window h :maximize] {} _)))

(defmulti mouse-button-down browser-dispatch)

(defmethods mouse-button-down [:chrome :phantom] []
  (with-http-post [:session *session* :buttondown] nil _))

(defmulti mouse-button-up browser-dispatch)

(defmethods mouse-button-up [:chrome :phantom] []
  (with-http-post [:session *session* :buttondown] nil _))

(defmacro with-mouse-btn [& body]
  `(do
     (mouse-button-down)
     (try
       ~@body
       (finally
         (mouse-button-up)))))

(defmulti mouse-move-to browser-dispatch)

(defmacro skip-predicate [predicate & body]
  `(when-not (~predicate)
     ~@body))

(defmacro skip-phantom [& body]
  `(skip-predicate
    #(= (:browser *server*) :phantom)
    ~@body))

(defmethods mouse-move-to [:chrome :phantom]
  ([q] (with-el q el
         (with-http :post
           [:session *session* :moveto]
           {:element el} _)))
  ([x y] (with-http :post
           [:session *session* :moveto]
           {:xoffset x :yoffset y} _)))

(defn drag-and-drop [q-from q-to]
  (mouse-move-to q-from)
  (with-mouse-btn
    (mouse-move-to q-to)))

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

;; tests

(def host "127.0.0.1")
(def port 6666)

(defn fixture-browsers [f]

  ;; "-v"

  (with-proc p [["geckodriver" "--host" host "--port" port "--log" "fatal"]]
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

(deftest test-clear
  (let [url (-> "html/test.html" io/resource str)
        form "//form[@id='submit-test']"
        input "//input[@id='simple-input']"
        submit "//input[@id='simple-submit']"]
    (wait 3)
    ;; (wait-running :message "The server did not start.")
    (with-session
      (go url)
      (wait 1)
      (skip-phantom
       (maximize))

      (wait 1)
      (is 1)
      ;; (testing "simple clear"
      ;;   (with-xpath
      ;;     (fill input "test")
      ;;     (clear input)
      ;;     (click submit)
      ;;     (with-url url
      ;;       (is (str/ends-with? url "?login=&password=&message=")))))
      ;; (testing "form clear"
      ;;   (with-xpath
      ;;     (fill-form form {:login "Ivan"
      ;;                      :password "lalilulelo"
      ;;                      :message "long_text_here"})
      ;;     (clear-form form)
      ;;     (click submit)
      ;;     (with-url url
      ;;       (is (str/ends-with? url "?login=&password=&message=")))))
      )))
