(ns webdriver.api
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

(def ^:dynamic *browser*) ;; todo rename to driver
(def ^:dynamic *host*)
(def ^:dynamic *port*)
(def ^:dynamic *server*)
(def ^:dynamic *session*)
(def ^:dynamic *locator* "xpath")

;;
;; tools
;;

(defn- browser-dispatch [& _] *browser*)

(defn flip [f]
  (fn [& args]
    (apply f (reverse args))))

(defn random-port []
  (let [max-port 65536
        offset 1024]
    (+ (rand-int (- max-port offset))
       offset)))

(defmacro defmethods [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))

;;
;; http client
;;

(defmacro with-http [meth path data bind & body]
  `(let [~bind (client/call *host* *port* ~meth ~path ~data)]
     ~@body))

(defmacro with-http-get [path bind & body]
  `(with-http :get ~path nil ~bind
     ~@body))

(defmacro with-http-post [path body bind & body]
  `(with-http :post ~path ~body ~bind
     ~@body))

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

(defmacro with-locator [locator & body]
  `(binding [*locator* ~locator]
     ~@body))

(defmacro with-xpath [& body]
  `(with-locator "xpath"
     ~@body))

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

;;
;; find elements
;;

(defmulti get-el browser-dispatch)

(defmethod get-el :firefox [q]
  (with-http
    :post
    [:session *session* :element]
    {:using *locator* :value q}
    resp
    (-> resp :value first second)))

(defmethods get-el [:chrome :phantom] [q]
  (with-http
    :post
    [:session *session* :element]
    {:using *locator* :value q}
    resp
    (-> resp :value :ELEMENT)))

(defmacro with-el [q bind & body]
  `(let [~bind (get-el ~q)]
     ~@body))

(defmulti get-el-from browser-dispatch)

(defmethods get-el-from [:chrome :phantom] [el-parent q]
  (with-http :post
    [:session *session* :element el-parent :element]
    {:using *locator* :value q}
    resp
    (-> resp :value :ELEMENT)))

(defmethod get-el-from :firefox [el-parent q]
  (with-http :post
    [:session *session* :element el-parent :element]
    {:using *locator* :value q}
    resp
    (-> resp :value first second)))

(defmulti get-els-from browser-dispatch)

(defmethods get-els-from [:chrome :phantom] [el-parent q]
  (with-http :post
    [:session *session* :element el-parent :elements]
    {:using *locator* :value q}
    resp
    (->> resp :value (mapv :ELEMENT))))

(defmethod get-els-from :firefox [el-parent q]
  (with-http :post
    [:session *session* :element el-parent :elements]
    {:using *locator* :value q}
    resp
    (->> resp :value (mapv (comp second first)))))

(defmacro with-el-from [el-parent q bind & body]
  `(let [~bind (get-el-from ~el-parent ~q)]
     ~@body))

(defmacro with-els-from [el-parent q bind & body]
  `(doseq [~bind (get-els-from ~el-parent ~q)]
     ~@body))

;;
;; tag name
;;

(defn tag-el [el]
  (with-http-get
    [:session *session* :element el :name]
    resp
    (:value resp)))

(defn tag [q]
  (with-el q el
    (tag-el el)))


;;
;; navigation
;;

(defn go [url]
  (with-http :post [:session *session* :url] {:url url} _))

(defn click [q]
  (with-el q el
    (with-http :post
      [:session *session* :element el :click] nil _)))

(defn back []
  (with-http :post [:session *session* :back] nil _))

(defn forward []
  (with-http :post [:session *session* :forward] nil _))

(defn refresh []
  (with-http :post [:session *session* :refresh] nil _))

(defn close []
  (with-http :delete [:session *session* :window] nil _))

;;
;; get URL
;;

(defn get-url []
  (with-http :get [:session *session* :url] nil resp
    (:value resp)))

(defmacro with-url [bind & body]
  `(let [~bind (get-url)]
     ~@body))

;;
;; session
;;

(defn new-session []
  (with-http :post [:session]
    {:desiredCapabilities {}
     :requiredCapabilities {}}
    resp
    (:sessionId resp)))

(defn delete-session []
  (with-http :delete [:session *session*] nil _))

(defmacro with-session [& body]
  `(binding [*session* (new-session)]
     (try
       ~@body
       (finally
         (delete-session)))))

;;
;; run webdriver
;;

(defn status []
  (with-http :get [:status] nil resp
    (:value resp)))

(defmacro with-browser [browser & body]
  `(binding [*browser* ~browser]
     ~@body))

(defmulti run-webdriver browser-dispatch)

(defmethod run-webdriver :firefox [port]
  (proc/run ["geckodriver" "--port" port]))

(defmethod run-webdriver :chrome [port]
  (proc/run ["chromedriver" (str "--port=" port) ]))

(defmethod run-webdriver :phantom [port]
  (proc/run ["phantomjs" "--webdriver" port]))

(defmacro with-webdriver [port & body]
  `(let [proc# (run-webdriver ~port)]
     (try
       ~@body
       (finally
         (proc/kill proc#)))))

(defmacro with-connect [host port & body]
  `(binding [*host* ~host
             *port* ~port]
     (let [msg# "The server did not start on %s:%s."]
       (wait-running :message (format msg# ~host ~port)))
     ~@body))

;;
;; windows
;;

(defmulti fullscreen browser-dispatch)

(defmethod fullscreen :firefox []
  (with-http-post
    [:session *session* :window :fullscreen] nil _))

(defmulti maximize browser-dispatch)

(defmethod maximize :firefox []
  (with-http :post
    [:session *session* :window :maximize] nil _))

(defmacro with-current-handle [bind & body]
  `(let [~bind (get-window-handle)]
     ~@body))

(defmulti get-window-handle browser-dispatch)

(defmethod get-window-handle :firefox []
  (with-http-get [:session *session* :window] resp
    (-> resp :value)))

(defmethods get-window-handle [:chrome :phantom] []
  (with-http-get [:session *session* :window_handle] resp
    (:value resp)))

(defmethod maximize :chrome []
  (with-current-handle h
    (with-http :post
      [:session *session* :window h :maximize] nil _)))

(defn switch-window [handle]
  (with-http :post
    [:session *session* :window]
    {:handle handle} _))

(defmulti window-handles browser-dispatch)

(defmethod window-handles :firefox []
  (with-http :get
    [:session *session* :window :handles]
    nil resp
    (:value resp)))

(defmethods window-handles [:chrome :phantom] []
  (with-http :get
    [:session *session* :window :window_handles]
    nil resp
    (:value resp)))

(defmacro with-window [handler & body]
  `(let [current# (get-window-handle)]
     (try
       (switch-window ~handler)
       ~@body
       (finally
         (switch-window current#)))))

(defmacro with-all-windows [& body]
  `(doseq [h# (window-handles)]
     (with-window h#
       ~@body)))

;;
;; mouse
;;

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

;;
;; skip/only browsers
;;

(defmacro skip-predicate [predicate & body]
  `(when-not (~predicate)
     ~@body))

(defmacro skip-browsers [browsers & body]
  `(skip-predicate
    #((set ~browsers) *browser*)
    ~@body))

(defmacro skip-phantom [& body]
  `(skip-browsers [:phantom]
                  ~@body))

(defmacro skip-firefox [& body]
  `(skip-browsers [:firefox]
                  ~@body))

(defmacro when-not-firefox [& body]
  `(skip-browsers [:firefox]
                  ~@body))

(defmacro skip-chrome [& body]
  `(skip-browsers [:chrome]
                  ~@body))

(defmacro when-predicate [predicate & body]
  `(when (~predicate)
     ~@body))

(defmacro when-browsers [browsers & body]
  `(when-predicate
       #((set ~browsers) *browser*)
     ~@body))

(defmacro when-firefox [& body]
  `(when-browsers [:firefox]
     ~@body))

(defmacro when-chrome [& body]
  `(when-browsers [:chrome]
     ~@body))

(defmacro when-phantom [& body]
  `(when-browsers [:phantom]
     ~@body))

;;
;; page title
;;

(defn get-title []
  (with-http :get [:session *session* :title] nil resp
    (:value resp)))

(defmacro with-title [bind & body]
  `(let [~bind (get-title)]
     ~@body))

;;
;; window size and position
;;

(defmulti get-window-size browser-dispatch)

(defmethod get-window-size :firefox []
  (with-http :get [:session *session* :window :size]
    nil resp
    (-> resp (select-keys [:width :height]))))

(defmethods get-window-size [:chrome :phantom] []
  (with-current-handle h
    (with-http :get [:session *session* :window h :size]
      nil resp
      (-> resp :value (select-keys [:width :height])))))

(defmulti set-window-size browser-dispatch)

(defmethods set-window-size [:chrome :phantom]
  ([{:keys [width height]}]
   (set-window-size width height))
  ([width height]
   (with-current-handle h
     (with-http :post [:session *session* :window h :size]
       {:width width :height height} _))))

(defmethod set-window-size :firefox
  ([{:keys [width height]}]
   (set-window-size width height))
  ([width height]
   (with-http :post [:session *session* :window :size]
     {:width width :height height} _)))

(defmacro with-window-size [width height & body]
  `(let [old# (get-window-size)]
     (set-window-size ~width ~height)
     (try
       ~@body
       (finally
         (set-window-size old#)))))

(defmacro let-window-size [bind & body]
  `(let [~bind (get-window-size)]
     ~@body))

;;
;; element location
;;

(defmulti el-location browser-dispatch)

(defmethods el-location [:chrome :phantom] [q]
  (with-el q el
    (with-http :get
      [:session *session* :element el :location]
      nil resp
      (-> resp :value (select-keys [:x :y])))))

(defmethod el-location :firefox [q]
  (with-el q el
    (with-http :get
      [:session *session* :element el :rect]
      nil resp
      (-> resp (select-keys [:x :y])))))

(defmacro with-el-location [q bind & body]
  `(let [~bind (el-location ~q)]
     ~@body))

;;
;; window position
;;

(defmulti get-window-position browser-dispatch)

(defmethods get-window-position [:chrome :phantom] []
  (with-current-handle h
    (with-http :get
      [:session *session* :window h :position]
      nil resp
      (-> resp :value (select-keys [:x :y])))))

(defmethod get-window-position :firefox []
  (with-http :get
    [:session *session* :window :position]
    nil resp
    (-> resp (select-keys [:x :y]))))

(defmacro let-window-position [bind & body]
  `(let [~bind (get-window-position)]
     ~@body))

(defmulti set-window-position browser-dispatch)

(defmethods set-window-position [:chrome :phantom]
  ([{:keys [x y]}]
   (set-window-position x y))
  ([x y]
   (with-current-handle h
     (with-http :post
       [:session *session* :window h :position]
       {:x x :y y} _))))

(defmethod set-window-position :firefox
  ([{:keys [x y]}]
   (set-window-position x y))
  ([x y]
   (with-http :post
     [:session *session* :window :position]
     {:x x :y y} _)))

(defmacro with-window-position [pos & body]
  `(let [old# (get-window-position)]
     (set-window-position ~pos)
     ~@body
     (set-window-position old#)))

;;
;; element size
;;

(defmulti el-size browser-dispatch)

(defmethods el-size [:chrome :phantom] [q]
  (with-el q el
    (with-http :get
      [:session *session* :element el :size]
      nil resp
      (-> resp :value (select-keys [:width :height])))))

(defmethod el-size :firefox [q]
  (with-el q el
    (with-http :get
      [:session *session* :element el :rect]
      nil resp
      (-> resp :value (select-keys [:width :height])))))

(defmacro with-el-size [q bind & body]
  `(let [~bind (el-size ~q)]
     ~@body))

(defn el-box [q]
  (let [{:keys [x y]} (el-location q)
        {:keys [width height]} (el-size)]
    {:x1 x
     :x2 (+ x width)
     :y1 y
     :y2 (+ y height)
     :width width
     :height height}))

(defmacro with-el-box [q bind & body]
  `(let [~bind (el-box ~q)]
     ~@body))

(defn intersects? [q1 q2]
  (let [a (el-box q1)
        b (el-box q2)]
    (or (< (a :y1) (b :y2))
        (> (a :y2) (b :y1))
        (< (a :x2) (b :x1))
        (> (a :x1) (b :x2)))))

;;
;; touch api
;;

(defmulti touch-tap browser-dispatch)

(defmethod touch-tap :chrome []
  (with-http :post [:session *session* :touch :click] nil _))

(defmulti touch-move browser-dispatch)

(defmethod touch-move :chrome [q]
  (with-el-location q {:keys [x y]}
    (with-http :post
      [:session *session* :touch :move]
      {:x x :y y} _)))

(defmulti touch-down browser-dispatch)

(defmethod touch-down :chrome []
  (with-http :post [:session *session* :touch :down] nil _))

(defmulti touch-up browser-dispatch)

(defmethod touch-up :chrome []
  (with-http :post [:session *session* :touch :up] nil _))

(defmacro with-touch [& body]
  `(try
     (touch-down)
     ~@body
     (finally
       (touch-up))))

(defmacro swipe [q-from q-to]
  `(do
     (touch-move q-from)
     (with-touch
       (touch-move q-to))))

;;
;; element attributes
;;

(defn attr-el [el name]
  (with-http :get
    [:session *session* :element el :attribute name]
    nil resp
    (:value resp)))

(defn attr [q name]
  (with-el q el
    (attr-el el name)))

(defmacro with-attr-el [el name & body]
  `(let [~name (attr-el ~el ~(str name))]
     ~@body))

(defmacro with-attr [term name & body]
  `(with-el ~term el#
     (with-attr-el el# ~name
       ~@body)))

(defmacro with-attrs-el [el names & body]
  (let [func (fn [name] `(attr-el ~el ~(str name)))
        forms (map func names)
        binds (-> names
                  (interleave forms)
                  vec
                  vector)]
    `(let ~@binds
       ~@body)))

(defmacro with-attrs [term names & body]
  `(with-el ~term el#
     (with-attrs-el el# ~names
       ~@body)))

;;
;; element css
;;

(defn css-el [el name]
  (with-http :get
    [:session *session* :element el :css name]
    nil resp
    (-> resp :value not-empty)))

(defn css [q name]
  (with-el q el
    (css-el el name)))

(defmacro with-css-el [el name & body]
  `(let [~name (css-el ~el ~(str name))]
     ~@body))

(defmacro with-css [q name & body]
  `(with-el ~q el#
     (with-css-el el# ~name
       ~@body)))

(defmacro with-csss-el [el names & body]
  (let [func (fn [name] `(css-el ~el ~(str name)))
        forms (map func names)
        binds (-> names
                  (interleave forms)
                  vec
                  vector)]
    `(let ~@binds
       ~@body)))

(defmacro with-csss [q names & body]
  `(with-el ~q el#
     (with-csss-el el# ~names
       ~@body)))

;;
;; alerts
;;

(defmulti dismiss-alert browser-dispatch)

(defmethod dismiss-alert :chrome []
  (with-http :post
    [:session *session* :dismiss_alert] nil _))

(defmethod dismiss-alert :firefox []
  (with-http :post
    [:session *session* :alert :dismiss] nil _))

(defmulti accept-alert browser-dispatch)

(defmethod accept-alert :chrome []
  (with-http :post
    [:session *session* :accept_alert] nil _))

(defmethod accept-alert :firefox []
  (with-http :post
    [:session *session* :alert :accept] nil _))

(defmulti get-alert-text browser-dispatch)

(defmethod get-alert-text :chrome []
  (with-http :get
    [:session *session* :alert_text] nil resp
    (:value resp)))

(defmethod get-alert-text :firefox []
  (with-http :get
    [:session *session* :alert :text] nil resp
    (:value resp)))

(defmacro with-alert-text [bind & body]
  `(let [~bind (get-alert-text)]
     ~@body))

;;
;; active element
;;

(defmulti get-active-el browser-dispatch)

(defmethods get-active-el [:chrome :phantom] []
  (with-http :post [:session *session* :element :active]
    nil resp
    (-> resp :value :ELEMENT)))

(defmethod get-active-el :firefox []
  (with-http :get [:session *session* :element :active]
    nil resp
    (-> resp :value first second)))

(defmacro let-active-el [bind & body]
  `(let [~bind (get-active-el)]
     ~@body))

;;
;; element text
;;

(defn text-el [el]
  (with-http :get
    [:session *session* :element el :text] nil resp
    (:value resp)))

(defn text [q]
  (with-el q el
    (text-el el)))

(defmacro with-text [q bind & body]
  `(let [~bind (text ~q)]
     ~@body))

;;
;; element value
;;

(defmulti value-el browser-dispatch)

(defmethod value-el :default [el]
  (with-http :get
    [:session *session* :element el :value]
    nil resp
    (:value resp)))

(defn value [q]
  (with-el q el
    (value-el el)))

(defmacro with-value [q bind & body]
  `(let [~bind (value ~q)]
     ~@body))

;;
;; cookies
;;

(defmulti get-cookies browser-dispatch)

(defmethod get-cookies :default []
  (with-http :get
    [:session *session* :cookie]
    nil resp
    (:value resp)))

(defmulti get-named-cookies browser-dispatch)

(defmacro with-cookies [bind & body]
  `(let [~bind (get-cookies)]
     ~@body))

(defmethod get-named-cookies :firefox [name]
  (with-http :get
    [:session *session* :cookie name]
    nil resp
    (:value resp)))

(defmethod get-named-cookies :chrome [name]
  (with-cookies [cookies]
    (filterv #(-> % :name (= name)) cookies)))

(defmethod get-named-cookies :phantom [name]
  (with-cookies [cookies]
    (filterv #(-> % :name (= name)) [cookies])))

(defmacro with-named-cookies [name bind & body]
  `(let [~bind (get-named-cookies ~name)]
     ~@body))

(defmulti set-cookie browser-dispatch)

(defmethod set-cookie :default [cookie]
  (with-http :post
    [:session *session* :cookie]
    {:cookie cookie} _))

(defmulti delete-cookie browser-dispatch)

(defmethod delete-cookie :default [name]
  (with-http :delete
    [:session *session* :cookie name]
    nil _))

(defmulti delete-cookies browser-dispatch)

(defmethod delete-cookies :default []
  (with-http :delete
    [:session *session* :cookie]
    nil _))

;;
;; source code
;;

(defmulti get-source browser-dispatch)

(defmethod get-source :default []
  (with-http :get
    [:session *session* :source]
    nil resp
    (:value resp)))

(defmacro let-source [bind & body]
  `(let [~bind (get-source)]
     ~@body))

;;
;; element property
;;

(defmulti prop-el browser-dispatch)

(defmethod prop-el :firefox [el name]
  (with-http :get
    [:session *session* :element el :property name]
    nil resp
    (-> resp :value not-empty)))

(defn prop [q name]
  (with-el q el
    (prop-el el name)))

(defmacro let-prop [q name & body]
  `(let [~name (prop ~q ~(str name))]
     ~@body))

(defmacro let-props-el [el names & body]
  (let [func (fn [name] `(prop-el ~el ~(str name)))
        forms (map func names)
        binds (-> names
                  (interleave forms)
                  vec)]
    `(let [~@binds]
       ~@body)))

(defmacro let-props [q names & body]
  `(with-el ~q el#
     (let-props-el el# ~names ~@body)))

;;
;; screenshot
;;

(defn b64-to-file [b64str filename]
  (with-open [out (io/output-stream filename)]
    (.write out (-> b64str
                    .getBytes
                    b64/decode))))

(defmulti screenshot browser-dispatch)

(defmethod screenshot :default [filename]
  (with-http :get
    [:session *session* :screenshot]
    nil resp
    (-> resp
        :value
        not-empty
        (or (throw+ {:type :webdriver/screenshot
                     :message "Empty screenshot"
                     :session *session*
                     :host *host*
                     :port *port*}))
        (b64-to-file filename))))

;;
;; javascript
;;

(defmulti js-execute browser-dispatch)

(defmethods js-execute [:chrome :phantom] [script & args]
  (with-http :post
    [:session *session* :execute]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defmethod js-execute :firefox [script & args]
  (with-http :post
    [:session *session* :execute :sync]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defn js-add-script [url]
  (let [script (str "var s = document.createElement('script');"
                    "s.type = 'text/javascript';"
                    "s.src = arguments[0];"
                    "document.head.appendChild(s);")]
    (js-execute script url)))

(defn js-clear-local-storage []
  (js-execute "localStorage.clear();"))

(defn js-set-hash [hash]
  (js-execute "window.location.hash = arguments[0];" hash))

;;
;; predicates
;;

(defn visible-el [el]
  (with-http :get
    [:session *session* :element el :displayed]
    nil resp
    (:value resp)))

(defn visible [q]
  (with-el q el
    (visible-el el)))

(defn running []
  (with-conn-error
    (status)))

(defn- enabled-el [el]
  (with-http :get
    [:session *session* :element el :enabled]
    nil resp
    (:value resp)))

(defn enabled [q]
  (with-el q el
    (enabled-el el)))

(def disabled (complement enabled))

(defn exists-el [el]
  (with-http-error
    (tag-el el)
    true))

(defn exists [q]
  (with-http-error
    (with-el q el
      true)))

(def not-exists (complement exists))

(defn has-text [text]
  (with-http-error
    (let [q (format "//*[contains(text(),'%s')]" text)]
      (with-el q el
        true))))

(defn has-class-el [el class-name]
  (let [classes (attr-el el "class")]
    (cond
      (nil? classes) false
      (string? classes)
      (-> classes
          (str/split #"\s+")
          set
          (get class-name)))))

(defn has-class [q class-name]
  (with-el q el
    (has-class-el el class-name)))

(defn has-alert []
  (with-http-error
    (get-alert-text)
    true))

;;
;; wait functions
;;

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

(defn wait-for-predicate
  [predicate & {:keys [timeout poll message]
                :or {timeout 10 poll 0.5}}]
  (loop [times 0
         time-rest timeout]
    (when (< time-rest 0)
      (throw+ {:type :webdriver/timeout
               :message message
               :timeout timeout
               :poll poll
               :times times
               :predicate predicate}))
    (when-not (predicate)
      (wait poll)
      (recur (inc times)
             (- time-rest poll)))))

(defn wait-enabled [q & args]
  (apply wait-for-predicate #(enabled q) args))

(defn wait-exists [q & args]
  (apply wait-for-predicate #(exists q) args))

(defn wait-visible [q & args]
  (apply wait-for-predicate #(visible q) args))

(defn wait-has-alert [& args]
  (apply wait-for-predicate has-alert args))

(defn wait-running [& args]
  (apply wait-for-predicate running args))

(defn wait-has-text [text & args]
  (apply wait-for-predicate #(has-text text) args))

(defn wait-for-has-class [q class & args]
  (apply wait-for-predicate #(has-class q class) args))

;;
;; fill and input
;;

(defn fill-el [el text]
  (with-http :post
    [:session *session* :element el :value]
    {:value (vec text)} _))

(defn fill [q text]
  (with-el q el
    (fill-el el text)))

(defn fill-form [q form]
  (with-el q el-form
    (doseq [[field value] form]
      (let [q-field (format ".//*[@name='%s']" (name field))
            text (str value)]
        (with-xpath
          (with-el-from el-form q-field el-field
            (fill-el el-field text)))))))

(defn fill-human-el [el text]
  (let [mistake-prob 0.1
        pause-max 0.2
        rand-char #(-> 26 rand-int (+ 97) char)
        wait-key #(let [r (rand)]
                    (wait (if (> r pause-max) pause-max r)))]
    (doseq [key text]
      (when (< (rand) mistake-prob)
        (fill-el el (rand-char))
        (wait-key)
        (fill-el el keys/backspace)
        (wait-key))
      (fill-el el key)
      (wait-key))))

(defn fill-human [q text]
  (with-el q el
    (fill-human-el el text)))

(defn clear-el [el]
  (with-http :post
    [:session *session* :element el :clear]
    nil _))

(defn clear [q]
  (with-el q el
    (clear-el el)))

(defn- clear-form-el [el-form]
  (with-xpath
    (doseq [q [".//textarea"
               ".//input[@type='text']"
               ".//input[@type='password']"]]
      (with-els-from el-form q el-input
        (clear-el el-input)))))

(defn clear-form [q]
  (with-el q el-form
    (clear-form-el el-form)))

;;
;; submiting
;;

;; (defn- submit-form-el [el-form form]
;;   (fill-form-el el-form form)
;;   (with-xpath
;;     (with-el-from el-form ".//input[@type='submit']"
;;       el-submit
;;       (click-el el-submit))))

;; (defn submit-form [term form]
;;   (with-el term el-form
;;     (submit-form-el el-form form)))
