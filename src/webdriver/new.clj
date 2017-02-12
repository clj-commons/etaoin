(ns webdriver.new
  "
  The API below was written regarding to the source code
  of different Webdriver implementations.

  Sometimes, a feature you found out in W3C official standard
  really differs from the corresponding implementation in Chrome or Firefox, etc.

  Chrome:
  https://github.com/bayandin/chromedriver/blob/e9a1f55b166ea62ef0f6e78da899d9abf117e88f/client/command_executor.py

  Firefox (Geckodriver):
  https://github.com/mozilla/webdriver-rust/blob/7ec65451c99b638655c72e7b9718a374ff60de87/src/httpapi.rs

  Phantom.js (Ghostdriver)
  https://github.com/detro/ghostdriver/blob/873c9d660a80a3faa743e4f352571ce4559fe691/src/request_handlers/session_request_handler.js
  https://github.com/detro/ghostdriver/blob/873c9d660a80a3faa743e4f352571ce4559fe691/src/request_handlers/webelement_request_handler.js
  "
  (:require [clojure.string :as str]
            [webdriver.proc :as proc]
            [webdriver.client :as client]
            [webdriver.keys :as keys]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [cheshire.core :refer [parse-string]]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import java.net.ConnectException))

;;
;; defaults
;;

(def default-paths {:firefox "geckodriver"
                    :chrome "chromedriver"
                    :phantom "phantomjs"
                    :safari "safaridriver"})

(def default-ports {:firefox 4444
                    :chrome 5555
                    :phantom 8910})

;;
;; utils
;;

(defmacro defmethods
  "Declares multimethods in batch."
  [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))

(defn dispatch-driver [driver & _]
  (:type @driver))

(defn random-port
  "Returns a random port skiping first 1024 ones."
  []
  (let [max-port 65536
        offset 1024]
    (+ (rand-int (- max-port offset))
       offset)))

;;
;; api
;;

(defmacro with-resp [driver method path data result & body]
  `(let [~result (client/call
                  (-> ~driver deref :host)
                  (-> ~driver deref :port)
                  ~method
                  ~path
                  ~data)]
     ~@body))

(defn create-session [driver]
  (with-resp driver
    :post
    [:session]
    {:desiredCapabilities {}}
    result
    (:sessionId result)))

(defn delete-session [driver]
  (with-resp driver
    :delete
    [:session (:session @driver)]
    nil
    _))

;;
;; navigation
;;

(defn go [driver url]
  (with-resp driver :post
    [:session (:session @driver) :url]
    {:url url} _))

(defn back [driver]
  (with-resp driver :post
    [:session (:session @driver) :back]
    nil _))

(defn refresh [driver]
  (with-resp driver :post
    [:session (:session @driver) :refresh]
    nil _))

(defn forward [driver]
  (with-resp driver :post
    [:session (:session @driver) :forward]
    nil _))

;;
;; URL and title
;;

(defn get-url [driver]
  (with-resp driver :get
    [:session (:session @driver) :url]
    nil resp
    (:value resp)))

(defn get-title [driver]
  (with-resp driver :get
    [:session (:session @driver) :title]
    nil resp
    (:value resp)))

;;
;; find element(s)
;;

(defn by [driver locator]
  (swap! driver assoc :locator locator)
  driver)

(defmulti find dispatch-driver)

(defmethod find :firefox [driver q]
  (with-resp driver :post
    [:session (:session @driver) :element]
    {:using (:locator @driver) :value q}
    resp
    (-> resp :value first second)))

(defmethod find :default [driver q]
  (with-resp driver :post
    [:session (:session @driver) :element]
    {:using (:locator @driver) :value q}
    resp
    (-> resp :value :ELEMENT)))

;;
;; click
;;

(defn click-el [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :click]
    nil _))

(defn click [driver q]
  (click-el driver (find driver q)))

;;
;; element size
;;

(defmulti get-element-size-el dispatch-driver)

(defmethods get-element-size-el [:chrome :phantom :safari]
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :size]
    nil
    resp
    (-> resp :value (select-keys [:width :height]))))

(defmethod get-element-size-el :firefox
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :rect]
    nil
    resp
    (-> resp (select-keys [:width :height]))))

(defn get-element-size [driver q]
  (get-element-size-el driver (find driver q)))

;;
;; element location
;;

(defmulti get-element-location-el dispatch-driver)

(defmethods get-element-location-el
  [:chrome :phantom :safari]
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :location]
    nil
    resp
    (-> resp :value (select-keys [:x :y]))))

(defmethod get-element-location-el :firefox
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :rect]
    nil
    resp
    (-> resp (select-keys [:x :y]))))

(defn get-element-location [driver q]
  (get-element-location-el driver (find driver q)))

;;
;; wait functions
;;

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

;;
;; driver management
;;

(defn make-url [host port]
  (format "http://%s:%s" host port))

(defmulti port-args dispatch-driver)

(defmethods port-args [:firefox :safari] [driver]
  ["--port" (:port @driver)])

(defmethod port-args :chrome [driver]
  [(str "--port=" (:port @driver))])

(defmethod port-args :phantom [driver]
  ["--webdriver" (:port @driver)])

(defn create-driver [type & [opt]]
  (let [driver (atom {})
        host (or (:host opt) "127.0.0.1")
        port (or (:port opt)
                 (type default-ports)
                 (random-port))
        url (make-url host port)
        locator (or (:locator opt) "xpath")]
    (swap! driver assoc
           :type type
           :host host
           :port port
           :url url
           :locator locator)
    driver))

(defn run-driver [driver & [opt]]
  (let [type (:type @driver)
        path (or (:path opt)
                 (type default-paths))
        args (or (:args opt)
                 [])
        env (or (:env opt) {})
        port-args (port-args driver)
        full-args (vec (concat [path] port-args args))
        process (proc/run full-args env)]
    (swap! driver assoc
           :env env
           :args full-args
           :process process)
    driver))

(defn connect-driver [driver & [opt]]
  (wait 2)
  (let [session (create-session driver)]
    (swap! driver assoc :session session)
    driver))

(defn disconnect-driver [driver]
  (delete-session driver)
  (swap! driver dissoc :session)
  driver)

(defn stop-driver [driver]
  (proc/kill (:process @driver))
  (swap! driver dissoc :process :args :env)
  driver)

(defn boot-driver [type & [opt]]
  (-> type
      (create-driver opt)
      (run-driver opt)
      (connect-driver opt)))

(def firefox (partial boot-driver :firefox))
(def chrome (partial boot-driver :chrome))
(def phantom (partial boot-driver :phantom))
(def safari (partial boot-driver :safari))
