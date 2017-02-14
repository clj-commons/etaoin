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

;;
;; session and status
;;

(defn get-status [driver]
  (with-resp driver :get
    [:status]
    nil
    resp
    (:value resp)))

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
;; windows
;;

(defmulti get-window-handle dispatch-driver)

(defmethod get-window-handle :default
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window_handle]
    nil
    resp
    (:value resp)))

(defmethod get-window-handle :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window]
    nil
    resp
    (-> resp :value)))

(defmulti get-window-handles dispatch-driver)

(defmethod get-window-handles :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window :handles]
    nil resp
    (:value resp)))

(defmethods get-window-handles [:chrome :phantom]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window :window_handles]
    nil resp
    (:value resp)))

(defn switch-window [driver handle]
  (with-resp driver :post
    [:session (:session @driver) :window]
    {:handle handle} _))

(defmulti maximize dispatch-driver)

(defmethod maximize :firefox
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :window :maximize]
    nil _))

(defmethods maximize [:chrome :safari]
  [driver]
  (let [h (get-window-handle driver)]
    (with-resp driver :post
      [:session (:session @driver) :window h :maximize]
      nil _)))

;; size and pos

;;
;; mouse
;;

;;
;; touch
;;

;;
;; skip/only driver
;;

;;
;; input and submit
;;

;;
;; forms
;;

;;
;; screenshot
;;

;;
;; human actions
;;

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
;; element box
;;

(defn get-element-box [driver q]
  (let [el (find driver q)
        {:keys [width height]} (get-element-size-el driver el)
        {:keys [x y]} (get-element-location-el driver el)]
    {:x1 x
     :x2 (+ x width)
     :y1 y
     :y2 (+ y height)
     :width width
     :height height}))

(defn intersects? [driver q1 q2]
  (let [a (get-element-box driver q1)
        b (get-element-box driver q2)]
    (or (< (a :y1) (b :y2))
        (> (a :y2) (b :y1))
        (< (a :x2) (b :x1))
        (> (a :x1) (b :x2)))))

;;
;; attributes
;;

(defn get-element-attr-el [driver el name]
  (with-resp driver :get
    [:session (:session @driver) :element el :attribute name]
    nil
    resp
    (:value resp)))

(defn get-element-attr [driver q name]
  (get-element-attr-el driver (find driver q) name))

(defn get-element-attrs [driver q & names]
  (let [el (find driver q)]
    (mapv
     #(get-element-attr-el driver el %)
     names)))

;;
;; css
;;

(defn get-element-css-el [driver el name]
  (with-resp driver :get
    [:session (:session @driver) :element el :css name]
    nil
    resp
    (-> resp :value not-empty)))

(defn get-element-css [driver q name]
  (get-element-css-el driver (find driver q) name))

(defn get-element-csss [driver q & names]
  (let [el (find driver q)]
    (mapv
     #(get-element-css-el driver el %)
     names)))

;;
;; active element
;;

(defmulti get-active-el dispatch-driver)

(defmethods get-active-el [:chrome :phantom :safari]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :element :active]
    nil resp
    (-> resp :value :ELEMENT)))

(defmethod get-active-el :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :element :active]
    nil resp
    (-> resp :value first second)))

;;
;; element text, name and value
;;

(defn get-element-tag-el [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :name]
    nil
    resp
    (:value resp)))

(defn get-element-tag [driver q]
  (get-element-tag-el driver (find driver q)))

(defn get-element-text-el [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :text]
    nil
    resp
    (:value resp)))

(defn get-element-text [driver q]
  (get-element-text-el driver (find driver q)))

(defn get-element-value-el [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :value]
    nil
    resp
    (:value resp)))

(defn get-element-value [driver q]
  (get-element-value-el driver (find driver q)))

;;
;; cookes
;;

(defn get-cookies [driver]
  (with-resp driver :get
    [:session (:session @driver) :cookie]
    nil
    resp
    (:value resp)))

(defn get-named-cookie [driver name]
  (->> driver
       get-cookies
       (filter #(= (:name %) name))
       first))

(defn set-cookie [driver cookie]
  (with-resp driver :post
    [:session (:session @driver) :cookie]
    {:cookie cookie}
    _))

(defn delete-cookies [driver]
  (with-resp driver :delete
    [:session (:session @driver) :cookie]
    nil _))

;;
;; source code
;;

(defn get-source [driver]
  (with-resp driver :get
    [:session (:session @driver) :source]
    nil
    resp
    (:value resp)))

;;
;; execute js
;;

(defmulti js-execute dispatch-driver)

(defmethods js-execute [:default]
  [driver script & args]
  (with-resp driver :post
    [:session (:session @driver) :execute]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defmethod js-execute :firefox [driver script & args]
  (with-resp driver :post
    [:session (:session @driver) :execute :sync]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defn add-script [driver url]
  (let [script
        (str "var s = document.createElement('script');"
             "s.type = 'text/javascript';"
             "s.src = arguments[0];"
             "document.head.appendChild(s);")]
    (js-execute driver script url)))

;;
;; get/set hash
;;

(defn- split-hash [url]
  (str/split url #"#" 2))

(defn set-hash [driver hash]
  (let [[url _] (split-hash (get-url driver))
        new (format "%s#%s" url hash)]
    (go driver new)))

(defn get-hash [driver]
  (let [[_ hash] (split-hash (get-url driver))]
    hash))

;;
;; exceptions
;;

;;
;; exceptions
;;

(defmacro with-exception [catch fallback & body]
  `(try+
    ~@body
    (catch ~catch ~(quote _)
      ~fallback)))

(defmacro with-http-error [& body]
  `(with-exception [:type :webdriver/http-error] false
     ~@body))

(defmacro with-conn-error [& body]
  `(with-exception ConnectException false
     ~@body))

;;
;; locators
;;

(defmacro with-locator [driver locator & body]
  `(let [old# (-> ~driver deref :locator)]
     (swap! ~driver assoc :locator ~locator)
     (try
       ~@body
       (finally
         (swap! ~driver assoc :locator old#)))))

(defmacro with-xpath [driver & body]
  `(with-locator ~driver "xpath"
     ~@body))

;;
;; alerts
;;

(defmulti get-alert-text dispatch-driver)

(defmethod get-alert-text :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :alert :text]
    nil
    resp
    (:value resp)))

(defmethods get-alert-text [:chrome :safari]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :alert_text]
    nil
    resp
    (:value resp)))

(defmulti dismiss-alert dispatch-driver)

(defmethod dismiss-alert :firefox
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :alert :dismiss]
    nil _))

(defmethods dismiss-alert [:chrome :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :dismiss_alert]
    nil _))

(defmulti accept-alert dispatch-driver)

(defmethod accept-alert :firefox
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :alert :accept]
    nil _))

(defmethods accept-alert [:chrome :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :accept_alert]
    nil _))

;;
;; predicates
;;

(defn exists? [driver q]
  (with-http-error
    (get-element-text driver q)
    true))

(defn visible-el [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :displayed]
    nil
    resp
    (:value resp)))

(defn displayed? [driver q]
  (visible-el driver (find driver q)))

(defn visible? [driver q]
  (and (exists? driver q)
       (displayed? driver q)))

(def invisible? (complement visible?))

(defn enabled-el [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :enabled]
    nil
    resp
    (:value resp)))

(defn enabled? [driver q]
  (enabled-el driver (find driver q)))

(def disabled? (complement enabled?))

(defn has-text? [driver text]
  (with-http-error
    (let [q (format "//*[contains(text(),'%s')]" text)]
      (with-xpath driver
        (find driver q)
        true))))

(defn has-class-el [driver el class]
  (let [classes (get-element-attr-el driver el "class")]
    (cond
      (nil? classes) false
      (string? classes)
      (str/includes? classes class))))

(defn has-class? [driver q class]
  (has-class-el driver (find driver q) class))

(def has-no-class? (complement has-class?))

(defn has-alert? [driver]
  (with-http-error
    (get-alert-text driver)
    true))

(def has-no-alert? (complement has-alert?))

;;
;; wait functions
;;

(def default-timeout 10)
(def default-interval 0.1)

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

(defn wait-predicate
  ([pred]
   (wait-predicate pred {}))
  ([pred opt]
   (let [timeout (get opt :timeout default-timeout)
         interval (get opt :interval default-interval)
         times (get opt :times 0)
         message (get opt :message)]
     (when (< timeout 0)
       (throw+ {:type :webdriver/timeout
                :message message
                :timeout timeout
                :interval interval
                :times times
                :predicate pred}))
     (when-not (pred)
       (wait interval)
       (recur pred (assoc
                    opt
                    :timeout (- timeout interval)
                    :times (inc times)))))))

(defn wait-exists [driver q & [opt]]
  (wait-predicate #(exists? driver q) opt))

(defn wait-visible [driver q & [opt]]
  (wait-predicate #(visible? driver q) opt))

(defn wait-enabled [driver q & [opt]]
  (wait-predicate #(enabled? driver q) opt))

(defn wait-has-alert [driver & [opt]]
  (wait-predicate #(has-alert? driver) opt))

(defn wait-has-text [driver text & [opt]]
  (wait-predicate #(has-text? driver text) opt))

(defn wait-has-class [driver q class & [opt]]
  (wait-predicate #(has-class? driver q class) opt))

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
