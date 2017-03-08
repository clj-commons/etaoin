(ns etaoin.api
  "
  The API below was written regarding to the source code
  of different Webdriver implementations. All of them partially
  differ from the official W3C specification.

  The standard:
  https://www.w3.org/TR/webdriver/

  Chrome:
  https://github.com/bayandin/chromedriver/

  Firefox (Geckodriver):
  https://github.com/mozilla/webdriver-rust/

  Phantom.js (Ghostdriver)
  https://github.com/detro/ghostdriver/blob/
  "
  (:require [etaoin.proc :as proc]
            [etaoin.client :as client]
            [etaoin.keys :as keys]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [slingshot.slingshot :refer [try+ throw+]]))

;;
;; defaults
;;

(def default-paths
  "Default commands to launch a driver process."
  {:firefox "geckodriver"
   :chrome "chromedriver"
   :phantom "phantomjs"
   :safari "safaridriver"})

(def default-ports
  "Default ports to launch a driver process."
  {:firefox 4444
   :chrome 5555
   :phantom 8910})

(def default-locator "xpath")

;;
;; utils
;;

(defmacro defmethods
  "Declares multimethods in batch. For each dispatch value from
  dispatch-vals, creates a new method."
  [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))

(defn random-port
  "Returns a random port skiping the first 1024 ones."
  []
  (let [max-port 65536
        offset 1024]
    (+ (rand-int (- max-port offset))
       offset)))

(defn dispatch-driver
  "Returns the current driver's type. Used as dispatcher in
  multimethods."
  [driver & _]
  (:type @driver))

;;
;; api
;;

(defmacro with-resp
  "Executes an HTTP request to a driver's server. Performs the body
  within result data bound to the `result` clause.

  Arguments:

  - `driver`: a driver instance,

  - `method`: a keyword represents HTTP method, e.g. `:get`, `:post`,
  `:delete`, etc.

  - sdfsdf  `dsfsd`

  - `path`: a vector of strings/keywords represents a server's
  path. For example:

  `[:session \"aaaa-bbbb-cccc\" :element \"dddd-eeee\" :find]`

  will turn into \"/session/aaaa-bbbb-cccc/element/dddd-eeee/find\".

  - `data`: any data sctructure to be sent as JSON body. Put `nil` For
  `GET` requests.

  - `result`: a symbol to bind the data from the HTTP response with
  `let` form before executing the body.

  Example:

  (def driver (firefox))
  (with-resp driver :get
    [:session (:session @driver) :element :active]
    nil resp
    (print resp))
"
  [driver method path data result & body]
  `(let [~result (client/call ~driver
                              ~method
                              ~path
                              ~data)]
     ~@body))

;;
;; session and status
;;

(defn get-status
  "Returns the current Webdriver status info. The content depends on
  specific driver."
  [driver]
  (with-resp driver :get
    [:status]
    nil resp
    (:value resp)))

(defn create-session
  "Initiates a new session for a driver. Opens a browser window as a
  side-effect. All the further requests are made within specific
  session. Some drivers may work with only one active session. Returns
  a long string identifier."
  [driver]
  (with-resp driver
    :post
    [:session]
    {:desiredCapabilities {}}
    result
    (:sessionId result)))

(defn delete-session [driver]
  "Deletes a session. Closes a browser window."
  (with-resp driver
    :delete
    [:session (:session @driver)]
    nil _))

;;
;; actice element
;;

(defmulti ^:private get-active-element*
  "Returns the currect active element selected by mouse or a
  keyboard (Tab, arrows)."
  dispatch-driver)

(defmethod get-active-element* :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :element :active]
    nil resp
    (-> resp :value first second)))

(defmethods get-active-element* [:chrome :phantom :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :element :active]
    nil resp
    (-> resp :value :ELEMENT)))

;;
;; windows
;;

(defmulti get-window-handle
  "Returns the current active window handler as a string."
  dispatch-driver)

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

(defmulti get-window-handles
  "Returns a vector of all window handlers."
  dispatch-driver)

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

(defn switch-window
  "Switches a browser to another window."
  [driver handle]
  (with-resp driver :post
    [:session (:session @driver) :window]
    {:handle handle} _))

(defmulti close-window
  "Closes the current browser window."
  dispatch-driver)

(defmethod close-window :default
  [driver]
  (with-resp driver :delete
    [:session (:session @driver) :window]
    nil _))

(defmulti maximize
  "Makes the browser window as wide as your screen allows."
  dispatch-driver)

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

(defmulti get-window-size
  "Returns a window size a map with `:width` and `:height` keys."
  dispatch-driver)

(defmethod get-window-size :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window :size]
    nil
    resp
    (-> resp (select-keys [:width :height]))))

(defmethod get-window-size :default
  [driver]
  (let [h (get-window-handle driver)]
    (with-resp driver :get
      [:session (:session @driver) :window h :size]
      nil
      resp
      (-> resp :value (select-keys [:width :height])))))

(defmulti get-window-position
  "Returns a window position relative to your screen as a map with
  `:x` and `:y` keys."
  dispatch-driver)

(defmethod get-window-position :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window :position]
    nil
    resp
    (-> resp (select-keys [:x :y]))))

(defmethod get-window-position :default
  [driver]
  (let [h (get-window-handle driver)]
    (with-resp driver :get
      [:session (:session @driver) :window h :position]
      nil
      resp
      (-> resp :value (select-keys [:x :y])))))

(defmulti ^:private set-window-size* dispatch-driver)

(defmethod set-window-size* :firefox
  [driver width height]
  (with-resp driver :post
    [:session (:session @driver) :window :size]
    {:width width :height height} _))

(defmethod set-window-size* :default
  [driver width height]
  (let [h (get-window-handle driver)]
    (with-resp driver :post
      [:session (:session @driver) :window h :size]
      {:width width :height height} _)))

(defn set-window-size
  "Sets new size for a window. Absolute precision is not guaranteed."
  ([driver {:keys [width height]}]
   (set-window-size* driver width height))
  ([driver width height]
   (set-window-size* driver width height)))

(defmulti ^:private set-window-position* dispatch-driver)

(defmethod set-window-position* :firefox
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :window :position]
     {:x x :y y} _)))

(defmethod set-window-position* :default
  ([driver x y]
   (let [h (get-window-handle driver)]
     (with-resp driver :post
       [:session (:session @driver) :window h :position]
       {:x x :y y} _))))

(defn set-window-position
  "Sets new position for a window. Absolute precision is not
  guaranteed."
  ([driver {:keys [x y]}]
   (set-window-position* driver x y))
  ([driver x y]
   (set-window-position* driver x y)))

;;
;; navigation
;;

(defn go
  "Open the URL the current window.

  Example:

  (def ff (firefox))
  (go ff \"http://google.com\")
"
  [driver url]
  (with-resp driver :post
    [:session (:session @driver) :url]
    {:url url} _))

(defn back
  "Move backwards in a browser's history."
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :back]
    nil _))

(defn refresh
  "Reloads the current window."
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :refresh]
    nil _))

(defn forward
  "Move forwards in a browser's history."
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :forward]
    nil _))

;;
;; URL and title
;;

(defn get-url
  "Returns the current URL string."
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :url]
    nil resp
    (:value resp)))

(defn get-title
  "Returns the current window's title."
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :title]
    nil resp
    (:value resp)))

;;
;; find element(s)
;;

(defn q-xpath
  "Turns a map into an XPath clause. The rules are:

  - `:tag` value becomes a tag name, otherwise `*` is used;

  - `:index` becomes a `[x]` at the end of expression if passed;

  - any other key-value pair becomes an attribute filter as follows:
  `{:foo \"one\" :baz \"two\"}` => `\"[@foo='one'][@bar='two']\"`.

  - the final XPath is always relative (started with `.//`) to make it
  work with nested expressions.

  Example:

  (q-xpath {:tag :a :class :large :index 2 :target :_blank})
  > \".//a[@class='large'][@target='_blank'][2]\"
"
  [q]
  (let [tag (or (:tag q) :*)
        idx (:index q)
        attrs (dissoc q :tag :index)
        get-val (fn [val] (if (keyword? val)
                            (name val)
                            (str val)))
        pair (fn [[key val]] (format "[@%s='%s']"
                                     (name key)
                                     (get-val val)))
        parts (map pair attrs)
        xpath (apply str ".//" (name tag) parts)
        xpath (str xpath (if idx (format "[%s]" idx) ""))]
    xpath))

(defn q-expand
  "Expands a query expression into a pair of `[locator, term]` values
  to pass them into low-level HTTP API. Throws a Slingshot exception
  in case of unsupported clause."
  [driver q]
  (cond
    (string? q)
    [(:locator @driver) q]

    (and (map? q)
         (:xpath q))
    ["xpath" (:xpath q)]

    (and (map? q)
         (:css q))
    ["css selector" (:css q)]

    (map? q)
    ["xpath" (q-xpath q)]

    :else
    (throw+ {:type :etaoin/query
             :q q
             :driver @driver
             :message "Unsupported query clause"})))

(defmulti find-element* dispatch-driver)

(defmethod find-element* :firefox
  [driver locator term]
  (with-resp driver :post
    [:session (:session @driver) :element]
    {:using locator :value term}
    resp
    (-> resp :value first second)))

(defmethod find-element* :default
  [driver locator term]
  (with-resp driver :post
    [:session (:session @driver) :element]
    {:using locator :value term}
    resp
    (-> resp :value :ELEMENT)))

(defmulti find-element-from* dispatch-driver)

(defmethod find-element-from* :firefox
  [driver el locator term]
  (with-resp driver :post
    [:session (:session @driver) :element el :element]
    {:using locator :value term}
    resp
    (-> resp :value first second)))

(defmethod find-element-from* :default
  [driver el locator term]
  (with-resp driver :post
    [:session (:session @driver) :element el :element]
    {:using locator :value term}
    resp
    (-> resp :value :ELEMENT)))

(defn query
  "Finds an element on a page.

   A query might be:

   - a string, so the current browser's locator will be used. Examples:

   //div[@id='content'] for XPath,
   div.article for CSS selector

   - a keyword :active that means the current active element

   - a map with either :xpath or :css keys with a string term, e.g:
   {:xpath \"//div[@id='content']\"} or
   {:css \"div.article\"}

   - a map that will turn into an XPath expression:
   {:tag :div} => .//div
   {:id :container} => .//*[@id='container']
   {:tag :a :class :external :index 2} => .//a[@class='external'][2]

   - a vector of any clause mentioned above. In that case,
   every next term is searched inside the previous one. Example:
   [{:id :footer} {:tag :a}] => finds the first hyperlink
   inside a div with id 'footer'."
  [driver q]
  (cond
    (= q :active)
    (get-active-element* driver)

    (vector? q)
    (loop [el (query driver (first q))
           q-rest (rest q)]
      (if (empty? q-rest)
        el
        (let [q (first q-rest)
              [loc term] (q-expand driver q)]
          (recur (find-element-from* driver el loc term)
                 (rest q-rest)))))

    :else
    (let [[loc term] (q-expand driver q)]
      (find-element* driver loc term))))

;;
;; mouse
;;

(defmulti mouse-btn-down dispatch-driver)

(defmethods mouse-btn-down [:chrome :phantom :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :buttondown]
    nil _))

(defmulti mouse-btn-up dispatch-driver)

(defmethods mouse-btn-up [:chrome :phantom :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :buttonup]
    nil _))

(defmulti mouse-move-to dispatch-driver)

(defmethods mouse-move-to [:chrome :phantom :safari]
  ([driver q]
   (with-resp driver :post
     [:session (:session @driver) :moveto]
     {:element (query driver q)} _))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :moveto]
     {:xoffset x :yoffset y} _)))

(defmacro with-mouse-btn [driver & body]
  `(do
     (mouse-btn-down ~driver)
     (try
       ~@body
       (finally
         (mouse-btn-up ~driver)))))

(defn drag-and-drop [driver q-from q-to]
  (mouse-move-to driver q-from)
  (with-mouse-btn driver
    (mouse-move-to driver q-to)))

;;
;; click
;;

(defn click* [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :click]
    nil _))

(defn click [driver q]
  (click* driver (query driver q)))

(defmulti double-click* dispatch-driver)

(defmethods double-click* [:chrome :phantom]
  [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :doubleclick]
    nil _))

(defn double-click [driver q]
  (double-click* driver (query driver q)))

;;
;; element size
;;

(defmulti get-element-size* dispatch-driver)

(defmethods get-element-size* [:chrome :phantom :safari]
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :size]
    nil
    resp
    (-> resp :value (select-keys [:width :height]))))

(defmethod get-element-size* :firefox
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :rect]
    nil
    resp
    (-> resp (select-keys [:width :height]))))

(defn get-element-size
  "Returns an element size as a map with :width and :height keys."
  [driver q]
  (get-element-size* driver (query driver q)))

;;
;; element location
;;

(defmulti get-element-location* dispatch-driver)

(defmethods get-element-location*
  [:chrome :phantom :safari]
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :location]
    nil
    resp
    (-> resp :value (select-keys [:x :y]))))

(defmethod get-element-location* :firefox
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :rect]
    nil
    resp
    (-> resp (select-keys [:x :y]))))

(defn get-element-location [driver q]
  "Returns an element location on a page as a map with :x and :x keys."
  (get-element-location* driver (query driver q)))

;;
;; element box
;;

(defn get-element-box [driver q]
  (let [el (query driver q)
        {:keys [width height]} (get-element-size* driver el)
        {:keys [x y]} (get-element-location* driver el)]
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

(defn get-element-attr* [driver el attr]
  (with-resp driver :get
    [:session (:session @driver) :element el :attribute (name attr)]
    nil
    resp
    (:value resp)))

(defn get-element-attr [driver q name]
  (get-element-attr* driver (query driver q) name))

(defn get-element-attrs [driver q & names]
  (let [el (query driver q)]
    (mapv
     #(get-element-attr* driver el %)
     names)))

;;
;; css
;;

(defn get-element-css* [driver el name*]
  (with-resp driver :get
    [:session (:session @driver) :element el :css (name name*)]
    nil
    resp
    (-> resp :value not-empty)))

(defn get-element-css [driver q name]
  (get-element-css* driver (query driver q) name))

(defn get-element-csss [driver q & names]
  (let [el (query driver q)]
    (mapv
     #(get-element-css* driver el %)
     names)))

;;
;; active element
;;

(defmulti get-active* dispatch-driver)

(defmethods get-active* [:chrome :phantom :safari]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :element :active]
    nil resp
    (-> resp :value :ELEMENT)))

(defmethod get-active* :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :element :active]
    nil resp
    (-> resp :value first second)))

;;
;; element text, name and value
;;

(defn get-element-tag* [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :name]
    nil
    resp
    (:value resp)))

(defn get-element-tag [driver q]
  (get-element-tag* driver (query driver q)))

(defn get-element-text* [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :text]
    nil
    resp
    (:value resp)))

(defn get-element-text [driver q]
  (get-element-text* driver (query driver q)))

(defn get-element-value* [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :value]
    nil
    resp
    (:value resp)))

(defn get-element-value [driver q]
  (get-element-value* driver (query driver q)))

;;
;; cookes
;;

(defn get-cookies [driver]
  (with-resp driver :get
    [:session (:session @driver) :cookie]
    nil
    resp
    (:value resp)))

(defn get-cookie [driver cookie-name]
  (->> driver
       get-cookies
       (filter #(= (:name %) (name cookie-name)))
       first))

(defn set-cookie [driver cookie]
  (with-resp driver :post
    [:session (:session @driver) :cookie]
    {:cookie cookie}
    _))

(defn delete-cookie [driver cookie-name]
  (with-resp driver :delete
    [:session (:session @driver) :cookie (name cookie-name)]
    nil _))

(defmulti delete-cookies dispatch-driver)

(defmethod delete-cookies :default
  [driver]
  (with-resp driver :delete
    [:session (:session @driver) :cookie]
    nil _))

;; For unknown reason, Safari hangs forever when trying to delete
;; all cookies. Currently, we delete them in cycle.
(defmethod delete-cookies :safari
  [driver]
  (doseq [cookie (get-cookies driver)]
    (delete-cookie driver (:name cookie))))

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

(defmacro with-exception [catch fallback & body]
  `(try+
    ~@body
    (catch ~catch ~(quote _)
      ~fallback)))

(defmacro with-http-error [& body]
  `(with-exception [:type :etaoin/http-error] false
     ~@body))

;;
;; locators
;;


(defn by [driver locator]
  (swap! driver assoc :locator locator)
  driver)

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

(defmacro with-css [driver & body]
  `(with-locator ~driver "css selector"
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

(defn running? [driver]
  (with-exception Throwable false
    (let [{:keys [host port]} @driver
          socket (java.net.Socket. host port)]
      (if (.isConnected socket)
        (do
          (.close socket)
          true)
        false))))

(defn driver? [driver type]
  (= (dispatch-driver driver) type))

(defn chrome? [driver]
  (driver? driver :chrome))

(defn firefox? [driver]
  (driver? driver :firefox))

(defn phantom? [driver]
  (driver? driver :phantom))

(defn safari? [driver]
  (driver? driver :safari))

(defn exists? [driver q]
  (with-http-error
    (get-element-text driver q)
    true))

(def absent? (complement exists?))

(defmulti displayed* dispatch-driver)

(defmethod displayed* :default
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :displayed]
    nil
    resp
    (:value resp)))

(defmethod displayed* :safari
  [driver el]
  (cond
    (= (get-element-css* driver el :display)
       "none")
    false
    (= (get-element-css* driver el :visibility)
       "hidden")
    false
    :else true))

(defn displayed? [driver q]
  (displayed* driver (query driver q)))

(defn visible? [driver q]
  (and (exists? driver q)
       (displayed? driver q)))

(def invisible? (complement visible?))

(defn enabled* [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :enabled]
    nil
    resp
    (:value resp)))

(defn enabled? [driver q]
  (enabled* driver (query driver q)))

(def disabled? (complement enabled?))

(defn has-text? [driver text]
  (with-http-error
    (let [q (format "//*[contains(text(),'%s')]" text)]
      (query driver {:xpath q})
      true)))

(defn has-class* [driver el class]
  (let [classes (get-element-attr* driver el "class")]
    (cond
      (nil? classes) false
      (string? classes)
      (str/includes? classes (name class)))))

(defn has-class? [driver q class]
  (has-class* driver (query driver q) class))

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

(defn wait
  "Does nothing for N seconds."
  ([driver sec]
   (wait sec))
  ([sec]
   (Thread/sleep (* sec 1000))))

(defn wait-predicate
  ([pred]
   (wait-predicate pred {}))
  ([pred opt]
   (let [timeout (get opt :timeout default-timeout)
         time-rest (get opt :time-rest timeout)
         interval (get opt :interval default-interval)
         times (get opt :times 0)
         message (get opt :message)]
     (when (< time-rest 0)
       (throw+ {:type :etaoin/timeout
                :message message
                :timeout timeout
                :interval interval
                :times times
                :predicate pred}))
     (when-not (pred)
       (wait interval)
       (recur pred (assoc
                    opt
                    :time-rest (- time-rest interval)
                    :times (inc times)))))))

(defn wait-exists [driver q & [opt]]
  (wait-predicate #(exists? driver q) opt))

(defn wait-absent [driver q & [opt]]
  (wait-predicate #(absent? driver q) opt))

(defn wait-visible [driver q & [opt]]
  (wait-predicate #(visible? driver q) opt))

(defn wait-invisible [driver q & [opt]]
  (wait-predicate #(invisible? driver q) opt))

(defn wait-enabled [driver q & [opt]]
  (wait-predicate #(enabled? driver q) opt))

(defn wait-disabled [driver q & [opt]]
  (wait-predicate #(disabled? driver q) opt))

(defn wait-has-alert [driver & [opt]]
  (wait-predicate #(has-alert? driver) opt))

(defn wait-has-text [driver text & [opt]]
  (wait-predicate #(has-text? driver text) opt))

(defn wait-has-class [driver q class & [opt]]
  (wait-predicate #(has-class? driver q class) opt))

(defn wait-running [driver & [opt]]
  (wait-predicate #(running? driver) opt))

;;
;; touch
;;

(defmulti touch-tap dispatch-driver)

(defmethod touch-tap :chrome
  [driver q]
  (with-resp driver :post
    [:session (:session @driver) :touch :click]
    {:element (query driver q)} _))

(defmulti touch-down dispatch-driver)

(defmethod touch-down :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-down driver x y)))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :touch :down]
     {:x (int x) :y (int y)} _)))

(defmulti touch-up dispatch-driver)

(defmethod touch-up :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-down driver x y)))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :touch :up]
     {:x (int x) :y (int y)} _)))

(defmulti touch-move dispatch-driver)

(defmethod touch-move :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-move driver x y)))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :touch :move]
     {:x (int x) :y (int y)} _)))

;;
;; skip/when driver
;;

(defmacro when-not-predicate [predicate & body]
  `(when-not (~predicate)
     ~@body))

(defmacro when-not-chrome [driver & body]
  `(when-not-predicate #(chrome? ~driver) ~@body))

(defmacro when-not-phantom [driver & body]
  `(when-not-predicate #(phantom? ~driver) ~@body))

(defmacro when-not-firefox [driver & body]
  `(when-not-predicate #(firefox? ~driver) ~@body))

(defmacro when-not-safari [driver & body]
  `(when-not-predicate #(safari? ~driver) ~@body))

(defmacro when-predicate [predicate & body]
  `(when (~predicate)
     ~@body))

(defmacro when-chrome
  "Executes the body only if the driver is Chrome.

  Example:

  (def driver (chrome))
  (when-chrome driver
    (println \"It's Chrome!\")
"
  [driver & body]
  `(when-predicate #(chrome? ~driver) ~@body))

(defmacro when-phantom
  "Executes the body only if the driver is Phantom.js."
  [driver & body]
  `(when-predicate #(phantom? ~driver) ~@body))

(defmacro when-firefox
  "Executes the body only if the driver is Firefox."
  [driver & body]
  `(when-predicate #(firefox? ~driver) ~@body))

(defmacro when-safari
  "Executes the body only if the driver is Safari."
  [driver & body]
  `(when-predicate #(safari? ~driver) ~@body))

;;
;; input
;;

(defn- fill* [driver el text]
  (let [keys (if (char? text)
               (str text)
               text)]
    (with-resp driver :post
      [:session (:session @driver) :element el :value]
      {:value (vec keys)} _)))

(defn fill
  "Fills an element found with a query with a given text."
  [driver q text]
  (fill* driver (query driver q) text))

(defn- clear* [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :clear]
    nil _))

(defn clear
  "Clears an element (input, textarea) found with a query."
  [driver q]
  (clear* driver (query driver q)))

;;
;; submit
;;

(defn submit
  "Sends Enter button value to an element found with query."
  [driver q]
  (fill driver q keys/enter))

;;
;; forms
;;

;;
;; human actions
;;

(defn- fill-human* [driver el text]
  (let [mistake-prob 0.1
        pause-max 0.2
        rand-char #(-> 26 rand-int (+ 97) char)
        wait-key #(let [r (rand)]
                    (wait (if (> r pause-max) pause-max r)))]
    (doseq [key text]
      (when (< (rand) mistake-prob)
        (fill* driver el (rand-char))
        (wait-key)
        (fill* driver el keys/backspace)
        (wait-key))
      (fill* driver el key)
      (wait-key))))

(defn fill-human
  "Fills text like humans do: with error, corrections and pauses.

  Arguments:

  - `driver`: a driver instance,

  - `q`: a query term, see `query` function for more info,

  - `text`: a string to input."
  [driver q text]
  (fill-human* driver (query driver q) text))

;;
;; screenshot
;;

(defn b64-to-file [b64str filename]
  (with-open [out (io/output-stream filename)]
    (.write out (-> b64str .getBytes b64/decode))))

(defmulti screenshot
  "Takes a screenshot of the current page. Saves it in a *.png file on disk.
  Rises exception if a screenshot is empty.

  Arguments:

  - `driver`: driver instance,

  - `filename`: full path to a file."
  dispatch-driver)

(defmethod screenshot :default
  [driver filename]
  (with-resp driver :get
    [:session (:session @driver) :screenshot]
    nil
    resp
    (-> resp
        :value
        not-empty
        (or (throw+ {:type :etaoin/screenshot
                     :message "Empty screenshot"
                     :driver @driver}))
        (b64-to-file filename))))

;;
;; driver management
;;

(defn make-url
  "Makes an Webdriver URL from a host and port."
  [host port]
  (format "http://%s:%s" host port))

(defmulti port-args
  "Returns a vector of port arguments specific for each driver type."
  dispatch-driver)

(defmethods port-args [:firefox :safari] [driver]
  ["--port" (:port @driver)])

(defmethod port-args :chrome [driver]
  [(str "--port=" (:port @driver))])

(defmethod port-args :phantom [driver]
  ["--webdriver" (:port @driver)])

(defn create-driver
  "Creates a new driver instance.

  Returns an atom that represents driver's state. Some functions, for
  example creating or deleting a session may change its state.

  The function does not start a process or open a window. It just
  creates an atom without side effects.

  Arguments:

  - `type` is a keyword determines what driver to use. The supported
  browsers are `:firefox`, `:chrome`, `:phantom` and `:safari`.

  - `opt` is a map with additional options for a driver. The supported
  options are:

  -- `:host` is a string with either IP or hostname. Use it if the
  server is run not locally but somethere in your network.

  -- `:port` is an integer value what HTTP port to use. It is taken
  from the `default-ports` global map if is not passed. If there is no
  port in that map, a random-generated port is used.

  -- `:locator` is a string determs what algorithm to use by default
  when finding elements on a page. `default-locator` variable is used
  if not passed."
  [type & [opt]]
  (let [driver (atom {})
        host (or (:host opt) "127.0.0.1")
        port (or (:port opt)
                 (type default-ports)
                 (random-port))
        url (make-url host port)
        locator (or (:locator opt) default-locator)]
    (swap! driver assoc
           :type type
           :host host
           :port port
           :url url
           :locator locator)
    driver))

(defn run-driver
  "Runs a driver process locally.

  Creates a UNIX process with a Webdriver HTTP server. Host and port
  are taken from a `driver` argument. Updates a driver instance with
  new fields with process information. Returns modified driver.

  Arguments:

  - `driver` is an atom created with `create-driver` function.

  - `opt` is an optional map with the following possible parameters:

  -- `:path` is a string path to a binary file to
  launch. `default-paths` global map is used for lookup when not
  passed.

  -- `:args` is a vector of additional arguments passed when starting
  a process.

  -- `:env` is a map with system ENV variables. Keys are turned to
  upper-case strings."
  [driver & [opt]]
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

(defn connect-driver
  "Connects to a running Webdriver server.

  Creates a new session on Webdriver HTTP server. Sets the session to
  the driver. Returns the modified driver."
  [driver & [opt]]
  (wait-running driver)
  (let [session (create-session driver)]
    (swap! driver assoc :session session)
    driver))

(defn disconnect-driver
  "Disconnects from a running Webdriver server.

  Closes the current session that is stored in the driver. Removes the
  session from the driver instance. Returns modified driver."
  [driver]
  (delete-session driver)
  (swap! driver dissoc :session)
  driver)

(defn stop-driver
  "Stops the driver's process. Removes proces's data from the driver
  instance. Returns a modified driver."
  [driver]
  (proc/kill (:process @driver))
  (swap! driver dissoc :process :args :env)
  driver)

(defn boot-driver
  "Three-in-one: creates a driver, starts a process and creates a new
  session. Returns the driver instance.

  Arguments:

  - `type` a keyword determines a driver type.

  - `opt` a map of all possible parameters that `create-driver`,
  `run-driver` and `connect-driver` may accept.
"
  [type & [opt]]
  (-> type
      (create-driver opt)
      (run-driver opt)
      (connect-driver opt)))

(defn quit
  "Closes the current session and stops the driver."
  [driver]
  (try
    (disconnect-driver driver)
    (finally
      (stop-driver driver))))

(def firefox
  "Launches Firefox driver. A shortcut for `boot-driver`."
  (partial boot-driver :firefox))

(def chrome
  "Launches Chrome driver. A shortcut for `boot-driver`."
  (partial boot-driver :chrome))

(def phantom
  "Launches Phantom.js driver. A shortcut for `boot-driver`."
  (partial boot-driver :phantom))

(def safari
  "Launches Safari driver. A shortcut for `boot-driver`."
  (partial boot-driver :safari))

(defmacro with-driver
  "Performs the body within a driver session.

  Launches a driver of a given type. Binds the driver instance to a
  passed `bind` symbol. Executes the body once the driver has
  started. Shutdowns the driver finally (even if an exception
  occurred).

  Arguments:

  - `type` is a keyword what driver type to start.

  - `opt` is a map with driver's options. See `boot-driver` for more
  details.

  - `bind` is a symbol to bind a driver reference.

  Example:

  (with-driver :firefox {} driver
    (go driver \"http://example.com\"))
"
  [type opt bind & body]
  `(client/with-pool {}
     (let [~bind (boot-driver ~type ~opt)]
       (try
         ~@body
         (finally
           (quit ~bind))))))

(defmacro with-firefox
  "Performs the body with Firefox session. A shortcut for
  `with-driver`."
  [opt bind & body]
  `(with-driver :firefox ~opt ~bind
     ~@body))

(defmacro with-chrome
  "Performs the body with Chrome session. A shortcut for
  `with-driver`."
  [opt bind & body]
  `(with-driver :chrome ~opt ~bind
     ~@body))

(defmacro with-phantom
  "Performs the body with Phantom.js session. A shortcut for
  `with-driver`."
  [opt bind & body]
  `(with-driver :phantom ~opt ~bind
     ~@body))

(defmacro with-safari
  "Performs the body with Safari session. A shortcut for
  `with-driver`."
  [opt bind & body]
  `(with-driver :safari ~opt ~bind
     ~@body))
