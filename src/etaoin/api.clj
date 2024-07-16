(ns etaoin.api
  "A wrapper over the [W3C WebDriver Specification](https://www.w3.org/TR/webdriver/) to automate popular browsers.

  Tries to normalize differences across the various implementations.

  See the [User Guide](/doc/01-user-guide.adoc) for details and examples.

  This is a rich API:

  **WebDriver**
  - [[with-driver]] [[boot-driver]] [[defaults]] [[defaults-global]] [[when-not-drivers]]
  - [[with-chrome]] [[with-chrome-headless]] [[chrome]] [[chrome-headless]] [[chrome?]] [[when-chrome]] [[when-not-chrome]]
  - [[with-edge]] [[with-edge-headless]] [[edge]] [[edge-headless]] [[when-edge]] [[when-not-edge]]
  - [[with-firefox]] [[with-firefox-headless]] [[firefox]] [[firefox-headless]] [[firefox?]] [[when-firefox]] [[when-not-firefox]]
  - [[with-phantom]] [[phantom]] [[phantom?]] [[when-phantom]] [[when-not-phantom]]
  - [[with-safari]] [[safari]] [[safari?]] [[when-safari]] [[when-not-safari]]
  - [[driver?]] [[running?]] [[headless?]] [[when-headless]] [[when-not-headless]]
  - [[disconnect-driver]] [[stop-driver]] [[quit]]

  **WebDriver Lower Level Comms**
  -  [[execute]] [[with-http-error]]

  **Driver Sessions**
  - [[get-status]] [[create-session]] [[delete-session]]

  **Querying/Selecting DOM Elements**
  - [[query]] [[query-all]] [[query-tree]]
  - [[query-shadow-root]] [[query-shadow-root-el]] [[query-all-shadow-root]] [[query-all-shadow-root-el]]
  - [[has-shadow-root?]] [[has-shadow-root-el?]]
  - [[exists?]] [[absent?]]
  - [[displayed?]] [[displayed-el?]] [[enabled?]] [[enabled-el?]] [[disabled?]] [[invisible?]] [[visible?]]
  - [[child]] [[children]]
  - [[get-element-tag]] [[get-element-tag-el]]
  - [[get-element-attr]] [[get-element-attr-el]] [[get-element-attrs]]
  - [[get-element-property]] [[get-element-property-el]] [[get-element-properties]]
  - [[has-class?]] [[has-class-el?]] [[has-no-class?]]
  - [[get-element-css]] [[get-element-css-el]] [[get-element-csss]]
  - [[get-element-shadow-root]] [[get-element-shadow-root-el]]
  - [[get-element-text]] [[get-element-text-el]] [[has-text?]]
  - [[get-element-inner-html]] [[get-element-inner-html-el]]
  - [[get-element-value]] [[get-element-value-el]]
  - [[get-element-size]] [[get-element-size-el]] [[get-element-location]] [[get-element-location-el]] [[get-element-box]] [[intersects?]]
  - [[use-css]] [[with-css]] [[use-xpath]] [[with-xpath]]

  **Browser Navigation**
  - [[go]] [[get-url]] [[get-hash]] [[set-hash]]
  - [[back]] [[forward]]
  - [[refresh]] [[reload]]

  **Mouse/Pointer**
  - [[click]] [[click-el]] [[click-single]] [[click-multi]]
  - [[left-click]] [[left-click-on]]
  - [[middle-click]] [[middle-click-on]]
  - [[right-click]] [[right-click-on]]
  - [[mouse-click]] [[mouse-move-to]] [[mouse-click-on]]
  - [[double-click]] [[double-click-el]]
  - [[drag-and-drop]] [[mouse-btn-down]] [[mouse-btn-up]]
  - [[touch-down]] [[touch-move]] [[touch-tap]] [[touch-up]]

  **Inputs/Forms**
  - [[fill]] [[fill-active]] [[fill-el]] [[fill-multi]]
  - [[fill-human]] [[fill-human-el]] [[fill-human-multi]]
  - [[select]] [[selected?]] [[selected-el?]]
  - [[upload-file]] [[remote-file]]
  - [[disabled?]] [[enabled?]]
  - [[clear]] [[clear-el]]
  - [[submit]]

  **Cookies**
  - [[get-cookie]] [[get-cookies]]
  - [[set-cookie]]
  - [[delete-cookie]] [[delete-cookies]]

  **Alerts**
  - [[has-alert?]] [[has-no-alert?]]
  - [[get-alert-text]]
  - [[accept-alert]] [[dismiss-alert]]

  **Scrolling**
  - [[get-scroll]]
  - [[scroll]] [[scroll-by]]
  - [[scroll-bottom]] [[scroll-top]]
  - [[scroll-down]] [[scroll-up]] [[scroll-left]] [[scroll-right]]
  - [[scroll-offset]]
  - [[scroll-query]]

  **Scripting**
  - [[js-execute]] [[js-async]] [[js-localstorage-clear]] [[el->ref]] [[add-script]]

  **Browser Windows**
  - [[get-window-handle]] [[get-window-handles]]
  - [[get-window-position]] [[set-window-position]]
  - [[get-window-size]] [[set-window-size]]
  - [[maximize]]
  - [[switch-window]] [[switch-window-next]]
  - [[close-window]]

  **Frames**
  - [[switch-frame]] [[switch-frame-first]] [[switch-frame-parent]] [[switch-frame-top]] [[with-frame]]

  **Page Info**
  - [[get-source]] [[get-title]]

  **Screenshots**
  - [[screenshot]] [[screenshot-element]] [[with-screenshots]]

  **Print to PDF**
  - [[print-page]]

  **Browser Info**
  - [[supports-logs?]] [[get-log-types]] [[get-logs]]
  - [[get-user-agent]]
  - [[with-postmortem]]

  **Waiting**
  - [[doto-wait]] [[wait]] [[with-wait]]
  - [[*wait-interval*]] [[*wait-timeout*]] [[with-wait-interval]] [[with-wait-timeout]]
  - [[wait-exists]] [[wait-absent]]
  - [[wait-visible]] [[wait-invisible]]
  - [[wait-disabled]] [[wait-enabled]]
  - [[wait-has-alert]]
  - [[wait-has-class]]
  - [[wait-has-text]] [[wait-has-text-everywhere]]
  - [[wait-predicate]]
  - [[wait-running]]

  **Browser Timeouts**
  - [[get-implicit-timeout]] [[set-implicit-timeout]]
  - [[get-page-load-timeout]] [[set-page-load-timeout]]
  - [[get-script-timeout]] [[set-script-timeout]] [[with-script-timeout]]

  **WebDriver Actions**
  - [[make-action-input]] [[make-key-input]] [[make-mouse-input]] [[make-pen-input]] [[make-pointer-input]] [[make-touch-input]]
  - [[add-action]]
  - [[add-double-pause]]
  - [[add-key-down]] [[add-key-press]] [[add-key-up]] [[with-key-down]]
  - [[add-pause]]
  - [[add-pointer-click]] [[add-pointer-click-el]] [[add-pointer-double-click]] [[add-pointer-double-click-el]]
  - [[add-pointer-down]] [[add-pointer-up]]
  - [[with-pointer-btn-down]] [[with-pointer-left-btn-down]] [[with-mouse-btn]]
  - [[add-pointer-move]] [[add-pointer-move-to-el]]
  - [[add-pointer-cancel]]
  - [[perform-actions]] [[release-actions]]

  **Convenience**
  - [[rand-uuid]] [[when-predicate]] [[when-not-predicate]]
  "
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [etaoin.impl.client :as client]
   [etaoin.impl.driver :as drv]
   [etaoin.impl.proc :as proc]
   [etaoin.impl.util :as util :refer [defmethods]]
   [etaoin.impl.xpath :as xpath]
   [etaoin.keys :as k]
   [etaoin.query :as query]
   [slingshot.slingshot :refer [throw+ try+]])
  (:import
   java.text.SimpleDateFormat
   (java.util Base64 Date)))

(set! *warn-on-reflection* true)

;;
;; WebDriver defaults
;;
(def ^:no-doc default-locator "xpath")
(def ^:no-doc locator-xpath "xpath")
(def ^:no-doc locator-css "css selector")

(def ^{:doc "WebDriver global option defaults"} defaults-global
  {:log-level :all
   :locator default-locator
   :webdriver-failed-launch-retries 0})

(def ^{:doc "WebDriver driver type specific option defaults.
             Note that for locally launched WebDriver processes the default port is a random free port."}
  defaults
  {:firefox {:port 4444
             :path-driver "geckodriver"}
   :chrome  {:port 9515
             :path-driver "chromedriver"}
   :phantom {:port 8910
             :path-driver "phantomjs"}
   :safari  {:port 4445
             :path-driver "safaridriver"
             :webdriver-failed-launch-retries 4}
   :edge    {:port 17556
             :path-driver "msedgedriver"}})

;;
;; utils
;;

(defn ^:no-doc dispatch-driver
  "Returns the current driver's type. Used as dispatcher in
  multimethods."
  [driver & _]
  (:type driver))

(defn- implemented?
  [driver feature]
  (when (get-method feature (:type driver))
    true))

;;
;; api
;;

(defn execute
  "Return response from having `:driver` execute HTTP `:method` request to `:path` with body `:data`.

  Response body automatically converted from JSON to a clojure keywordized map.
  Any HTTP status failure code results in a throw.

  - `:method` HTTP method, e.g. `:get`, `:post`, `:delete`, etc.
  - `:path` a vector of strings/keywords representing a server's path. For example:
      - this: `[:session \"aaaa-bbbb-cccc\" :element \"dddd-eeee\" :find]`
      - becomes: `\"/session/aaaa-bbbb-cccc/element/dddd-eeee/find\"`.
  - `:data` optional body to send. Automatically converted to JSON.

  This can be useful to call when you want to invoke some WebDriver
  implementation specific feature that Etaoin has not otherwise exposed.

  For example, if Etaoin did not already have [[get-url]]:

  ```Clojure
  (def driver (e/firefox))
  (e/go driver \"https://clojure.org\")
  (e/execute {:driver driver
              :method :get
              :path [:session (:session driver) :url]})
  ;; => {:value \"https://clojure.org/\"}
  ```"
  [{:keys [driver method path data]}]
  (client/call driver method path data))

;;
;; session and status
;;

(defn get-status
  "Returns `driver` status.

  Can indicate readiness to create a new session.

  The return varies for different driver implementations."
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:status]})))

(defn create-session
  "Have `driver` create a new session and return resulting session-id string.

  Opens a browser window as a side-effect (visible if not running headless).
  Further requests to this driver will be for this session.
  Etaoin assumes one active session per driver."
  [driver & [capabilities]]
  (let [data   (if (= (dispatch-driver driver) :safari)
                 {:capabilities (or capabilities {})} ;; required for safari even if empty
                 {:desiredCapabilities (or capabilities {})})
        result (execute {:driver driver
                         :method :post
                         :path   [:session]
                         :data   data})]
    (or (:sessionId result)               ;; default
        (:sessionId (:value result)))))   ;; firefox, safari

(defn delete-session
  "Have `driver` delete the active session.
  Closes the browser window.

  See also: [[quit]]."
  [driver]
  (execute {:driver driver
            :method :delete
            :path   [:session (:session driver)]}))

;;
;; active element
;;

(defmulti ^:private get-active-element*
  "Have `driver` return the active element on the page.

  An active element is the one with the current focus.
  It was selected for example by mouse click, a keyboard (tab, arrows), or `autofocus`."
  dispatch-driver)

(defmethods get-active-element*
  [:firefox :safari]
  [driver]
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :element :active]})
      :value first second))

(defmethods get-active-element*
  [:chrome :edge :phantom]
  [driver]
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :element :active]})
      :value
      :ELEMENT))

;;
;; windows
;;

(defmulti get-window-handle
  "Have `driver` return the current browser window handle string."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod get-window-handle :default
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :window_handle]})))

(defmethods get-window-handle
  [:firefox :safari]
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :window]})))

(defmulti get-window-handles
  "Have `driver` return a vector of all browser window handle strings."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-window-handles
  [:firefox :safari]
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :window :handles]})))

(defmethods get-window-handles
  [:chrome :edge :phantom]
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :window_handles]})))

(defmulti switch-window
  "Have `driver` switch to browser window with `handle`."
  {:arglists '([driver handle])}
  dispatch-driver)

(defmethod switch-window
  :default
  [driver handle]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :window]
            :data   {:handle handle}}))

(defmethods switch-window
  [:chrome :edge :phantom]
  [driver handle]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :window]
            :data   {:name handle}}))

(defn switch-window-next
  "Have `driver` switch to next browser window."
  [driver]
  (let [current-handle (try
                         (get-window-handle driver)
                         (catch Exception _
                           (first (get-window-handles driver))))
        handles        (get-window-handles driver)
        next-handle    (loop [[h & hs] (cycle handles)]
                         (if (= h current-handle)
                           (first hs)
                           (recur hs)))]
    (switch-window driver next-handle)))

(defmulti close-window
  "Have `driver` close current browser window."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod close-window :default
  [driver]
  (execute {:driver driver
            :method :delete
            :path   [:session (:session driver) :window]}))

(defmulti maximize
  "Have `driver` make the current browser window as large as your screen allows."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods maximize
  [:firefox :safari]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :window :maximize]}))

(defmethods maximize
  [:chrome :edge]
  [driver]
  (let [h (get-window-handle driver)]
    (execute {:driver driver
              :method :post
              :path   [:session (:session driver) :window h :maximize]})))

(defmulti get-window-size
  "Have `driver` return the current browser window size in pixels as a map of `:width` and `:height`."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-window-size
  [:firefox :safari]
  [driver]
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :window :rect]})
      :value
      (select-keys [:width :height])))

(defmethod get-window-size :default
  [driver]
  (let [h (get-window-handle driver)]
    (-> (execute {:driver driver
                  :method :get
                  :path   [:session (:session driver) :window h :size]})
        :value
        (select-keys [:width :height]))))

(defmulti get-window-position
  "Have `driver` return the current window position, in pixels relative to the screen, as a map of
  `:x` and `:y`."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-window-position
  [:firefox :safari]
  [driver]
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :window :rect]})
      :value
      (select-keys [:x :y])))

(defmethod get-window-position :default
  [driver]
  (let [h (get-window-handle driver)]
    (-> (execute {:driver driver
                  :method :get
                  :path   [:session (:session driver) :window h :position]})
        :value
        (select-keys [:x :y]))))

(defmulti ^:private set-window-size* dispatch-driver)

(defmethod set-window-size* :firefox
  [driver width height]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :window :size]
            :data   {:width width :height height}}))

(defmethod set-window-size* :safari
  [driver width height]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :window :rect]
            :data   {:width width :height height}}))

(defmethod set-window-size* :default
  [driver width height]
  (let [h (get-window-handle driver)]
    (execute {:driver driver
              :method :post
              :path   [:session (:session driver) :window h :size]
              :data   {:width width :height height}})))

(defn set-window-size
  "Have `driver` set the `width` and `height` in pixels of the current window.
  Absolute precision is not guaranteed."
  ([driver {:keys [width height]}]
   (set-window-size* driver width height))
  ([driver width height]
   (set-window-size* driver width height)))

(defmulti ^:private set-window-position* dispatch-driver)

(defmethod set-window-position* :firefox
  ([driver x y]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :window :position]
             :data   {:x x :y y}})))

(defmethod set-window-position* :safari
  ([driver x y]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :window :rect]
             :data   {:x x :y y}})))

(defmethod set-window-position* :default
  ([driver x y]
   (let [h (get-window-handle driver)]
     (execute {:driver driver
               :method :post
               :path   [:session (:session driver) :window h :position]
               :data   {:x x :y y}}))))

(defn set-window-position
  "Have `driver` set the `x` `y` position of the current browser window.

  Position is in pixels and relative to your screen.
  Absolute precision is not guaranteed."
  ([driver {:keys [x y]}]
   (set-window-position* driver x y))
  ([driver x y]
   (set-window-position* driver x y)))


;;
;; navigation
;;

(defn go
  "Have `driver` open `url` in the current browser window.

  Example:

  ```Clojure
  (def ff (firefox))
  (go ff \"http://google.com\")
  ```"
  [driver url]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :url]
            :data   {:url url}}))

(defn back
  "Have `driver` navigate backward in the browser history."
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :back]}))

(defn refresh
  "Have `driver` reload the content in the current browser window."
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :refresh]}))

(def ^{:arglists '([driver])} reload "Alias for [[refresh]]" refresh)

(defn forward
  "Have `driver` navigate forward in the browser's history."
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :forward]}))

;;
;; URL and title
;;

(defn get-url
  "Have `driver` return the current url location as a string."
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :url]})))

(defn get-title
  "Have `driver` return the current page title."
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :title]})))

;;
;; Finding element(s)
;;

(defmulti ^:private find-element* dispatch-driver)

(defmethods find-element*
  [:firefox :safari]
  [driver locator term]
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :element]
                :data   {:using locator :value term}})
      :value
      first
      second))

(defmethod find-element* :default
  [driver locator term]
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :element]
                :data   {:using locator :value term}})
      :value :ELEMENT))

(defmulti ^:no-doc find-elements* dispatch-driver)

(defmethod find-elements* :default
  [driver locator term]
  (->> (execute {:driver driver
                 :method :post
                 :path   [:session (:session driver) :elements]
                 :data   {:using locator :value term}})
       :value
       (mapv (comp second first))))

(defmulti ^:private find-element-from* dispatch-driver)

(defmethods find-element-from*
  [:firefox :safari]
  [driver el locator term]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :element el :element]
                :data   {:using locator :value term}})
      :value
      first
      second))

(defmethod find-element-from* :default
  [driver el locator term]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :element el :element]
                :data   {:using locator :value term}})
      :value
      :ELEMENT))

(defmulti ^:private find-elements-from* dispatch-driver)

(defmethod find-elements-from* :default
  [driver el locator term]
  {:pre [(some? el)]}
  (->> (execute {:driver driver
                 :method :post
                 :path   [:session (:session driver) :element el :elements]
                 :data   {:using locator :value term}})
       :value
       (mapv (comp second first))))

;;
;; Querying elements (high-level API)
;;

(defn query
  "Use `driver` to find and return the first element on current page matching `q`.

   Query `q` can be:

   - `:active` the current active element
   - a keyword to find element by it's ID attribute:
     - `:my-id`
     - (use `{:id \"my-id\"}` for ids that cannot be represented as keywords)
   - an XPath expression:
     - `\".//input[@id='uname']\"`
   - a map with either `:xpath` or `:css`:
     - `{:xpath \".//input[@id='uname']\"`}`
     - `{:css \"input#uname[name='username']\"}`
   - any other map is converted to an XPath expression:
     - `{:tag :div}`
     - is equivalent to XPath: `\".//div\"`
   - multiple of the above (wrapped in a vector or not).
     The result of each expression is fed into the next.
     - `{:tag :div} \".//input[@id='uname']\"`
     - `[{:tag :div} \".//input[@id='uname']\"]`

   Returns the found element's unique identifier, or throws when not found.

   See [Selecting Elements](/doc/01-user-guide.adoc#querying) for more details."
  ([driver q]
   (cond

     (= q :active)
     (get-active-element* driver)

     (vector? q)
     (apply query driver q)

     :else
     (let [[loc term] (query/expand driver q)]
       (find-element* driver loc term))))

  ([driver q & more]
   (letfn [(folder [el q]
             (let [[loc term] (query/expand driver q)]
               (find-element-from* driver el loc term)))]
     (reduce folder (query driver q) more))))

(defn query-all
  "Use `driver` to return a vector of all elements on current page matching `q`.

  See [[query]] for details on `q`."
  ([driver q]
   (cond

     (vector? q)
     (apply query-all driver q)

     :else
     (let [[loc term] (query/expand driver q)]
       (find-elements* driver loc term))))

  ([driver q & more]
   (let [[loc term] (query/expand driver (last more))
         el         (apply query driver q (butlast more))]
     (find-elements-from* driver el loc term))))

(defn query-tree
  "Use `driver` to return a collection of all elements matching piped queries.

  The results of `q` are queried by `qs1` which in turn are queried by `qs2`, and so on.

  See [[query]] for details on `q`.

  See [User Guide](/doc/01-user-guide.adoc#query-tree) for an example."
  [driver q & qs]
  (reduce (fn [elements q]
            (let [[loc term] (query/expand driver q)]
              (set (mapcat (fn [e]
                             (find-elements-from* driver e loc term))
                           elements))))
          (let [[loc term] (query/expand driver q)]
            (find-elements* driver loc term))
          qs))

(defn child
  "Uses `driver` to return single element satisfying query `q` under given `ancestor-el` element.

  See [[query]] for details on `q`."
  [driver ancestor-el q]
  {:pre [(some? ancestor-el)]}
  (let [[loc term] (query/expand driver q)]
    (find-element-from* driver ancestor-el loc term)))

(defn children
  "Use `driver` to return a vector of unique elements satisfying query `q` under given `ancestor-el` element.

  See [[query]] for details on `q`."
  [driver ancestor-el q]
  {:pre [(some? ancestor-el)]}
  (let [[loc term] (query/expand driver q)]
    (find-elements-from* driver ancestor-el loc term)))

;; actions

(declare el->ref wait)

(defn rand-uuid
  "Return a random UUID string."
  []
  (str (java.util.UUID/randomUUID)))

(defn make-action-input
  "Return a new action input source of `type`."
  [type]
  {:type (name type) :id (rand-uuid) :actions []})

(defn make-pointer-input
  "Return a new pointer input source of pointer `type`."
  [type]
  (-> (make-action-input :pointer)
      (assoc-in [:parameters :pointerType] type)))

(defn make-mouse-input
  "Return a new mouse input source."
  []
  (make-pointer-input :mouse))

(defn make-touch-input
  "Return a new touch input source."
  []
  (make-pointer-input :touch))

(defn make-pen-input
  "Return a new pen input source."
  []
  (make-pointer-input :pen))

(defn make-key-input
  "Return a new key input source."
  []
  (make-action-input :key))

(defn add-action
  "Return `input` with `action` added."
  [input action]
  (update input :actions conj action))

(defn add-pause
  "Return `input` source with a pause action added.

  Optionally specify a `duration`, defaults to 0."
  [input & [duration]]
  (add-action input {:type "pause" :duration (or duration 0)}))

(defn add-double-pause
  "Return `input` source with two pause actions added.

  Optionally specify a `duration`, defaults to 0."
  [input & [duration]]
  (-> input
      (add-pause duration)
      (add-pause duration)))

(defn add-key-down
  "Return `input` source with `key` down action added."
  [input key]
  (add-action input {:type "keyDown" :value key}))

(defn add-key-up
  "Return `input` source with `key` up action added."
  [input key]
  (add-action input {:type "keyUp" :value key}))

(defn add-key-press
  "Return `input` source with `key` down and up actions added."
  [input key]
  (-> input
      (add-key-down key)
      (add-key-up key)))

(defn add-pointer-down
  "Return `input` source with `pointer-button` down action added.

  `button` defaults to [[etaoin.keys/mouse-left]]"
  [input & [pointer-button]]
  (add-action input {:type     "pointerDown"
                     :duration 0
                     :button   (or pointer-button k/mouse-left)}))

(defn add-pointer-up
  "Return `input` source with `pointer-button` up action added.

  `button` defaults to [[etaoin.keys/mouse-left]]"
  [input & [pointer-button]]
  (add-action input {:type     "pointerUp"
                     :duration 0
                     :button   (or pointer-button k/mouse-left)}))

(defn add-pointer-cancel
  "Return `input` with a pointer cancel action added."
  [input]
  (add-action input {:type "pointerCancel"}))

(defn add-pointer-move
  "Return `input` source with pointer move action added.

  Moves the pointer from `origin` to offset `x` `y` with optional `duration` in milliseconds.

  Possible `origin` values are:
  - `\"viewport\"` move to `x` `y` offset in viewport. This is the default.
  - `\"pointer\"` `x` `y` are interpreted as offsets from the current pointer location.
  - a map that represents a web element for example via [[el->ref]]:
      ```Clojure
      (el->ref (query driver q))
      ```
      where `q` is [[query]] to find the element.

  Optionally specify `:duration` in milliseconds, defaults to 250."
  [input & [{:keys [x y origin duration]}]]
  (let [default-duration 250
        default-origin "viewport"]
    (add-action input {:type     "pointerMove"
                       :x        (or x 0)
                       :y        (or y 0)
                       :origin   (or origin default-origin)
                       :duration (or duration default-duration)})))

(defn add-pointer-move-to-el
  "Return `input` source with pointer move to element `el` action added.

  Optionally specify `:duration` in milliseconds, defaults to 250."
  [input el & [{:keys [duration]}]]
  (add-pointer-move input {:duration duration
                           :origin   (el->ref el)}))

(defn add-pointer-click
  "Return `input` source with `pointer-button` down and up actions added.

  `pointer-button` defaults to [[etaoin.keys/mouse-left]]"
  [input & [pointer-button]]
  (-> input
      (add-pointer-down pointer-button)
      (add-pointer-up pointer-button)))

(defn add-pointer-click-el
  "Return `input` source with `pointer-button` down and up actions on element `el` added.

  `pointer-button` defaults to [[etaoin.keys/mouse-left]]"
  [input el & [pointer-button]]
  (-> input
      (add-pointer-move-to-el el)
      (add-pointer-click pointer-button)))

(defn add-pointer-double-click
  "Return `input` source with `pointer-button` down, up, down, up actions added.

  `pointer-button` defaults to [[etaoin.keys/mouse-left]]"
  [input & [pointer-button]]
  (-> input
      (add-pointer-click pointer-button)
      (add-pointer-click pointer-button)))

(defn add-pointer-double-click-el
  "Return `input` source with `pointer-button` down, up, down, up actions on element `el` added.

  `pointer-button` defaults to [[etaoin.keys/mouse-left]]"
  [input el & [pointer-button]]
  (-> input
      (add-pointer-move-to-el el)
      (add-pointer-double-click pointer-button)))

(defmacro with-key-down
  "Returns `input` source piped through `key` down,
  then presumably a `body` of more actions then a `key` up action."
  [input key & body]
  `(-> ~input
       (add-key-down ~key)
       ~@body
       (add-key-up ~key)))

(defmacro with-pointer-btn-down
  "Returns `input` source piped through `pointer-button` down action,
  then presumably a `body` of more actions then a pointer `pointer-button` up action.

  `pointer-button` should be, for example, [[etaoin.keys/mouse-left]]"
  [input pointer-button & body]
  `(-> ~input
       (add-pointer-down ~pointer-button)
       ~@body
       (add-pointer-up ~pointer-button)))

(defmacro with-pointer-left-btn-down
  "Returns `input` source piped through pointer left button down action,
  then presumably a `body` of more actions then a pointer left button up action."
  [input & body]
  `(-> ~input
       add-pointer-down
       ~@body
       add-pointer-up))

(defmulti perform-actions
  "Have `driver` perform actions defined in `input` source(s) simultaneously.

  See [Actions](/doc/01-user-guide.adoc#actions) for more details."
  {:arglists '([driver input & inputs])}
  dispatch-driver)

(defmethod perform-actions
  :default
  [driver input & inputs]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :actions]
            :data   {:actions (cons input inputs)}}))

(defmethod perform-actions
  :phantom
  [_driver _input & _inputs]
  (util/error "Phantom doesn't support w3c actions."))

(defmulti release-actions
  "Have `driver` clear any active action state.
  This includes any key presses and/or a pointer button being held down."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod release-actions
  :default
  [driver]
  (execute {:driver driver
            :method :delete
            :path   [:session (:session driver) :actions]}))

(defmethod release-actions
  :phantom
  [_driver _input & _inputs]
  (util/error "Phantom doesn't support w3c actions."))

;;
;; mouse
;;

(defmulti mouse-btn-down
  "Have `driver` press down on the button of the virtual mouse."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods mouse-btn-down
  [:chrome :edge :phantom]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :buttondown]}))

(defmulti mouse-btn-up
  "Have `driver` release button of the virtual mouse."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods mouse-btn-up
  [:chrome :edge :phantom]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :buttonup]}))

(defmulti mouse-move-to
  "Have `driver` moves the virtual mouse pointer either to an element found by query `q`
  or to a specific by `x` and `y` pixel offset within the browser window."
  {:arglists '([driver q] [driver x y])}
  dispatch-driver)

(defmethods mouse-move-to
  [:chrome :edge :phantom :firefox]
  ([driver q]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :moveto]
             :data   {:element (query driver q)}}))
  ([driver x y]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :moveto]
             :data   {:xoffset x :yoffset y}})))

(defmacro with-mouse-btn
  "Have `driver` press the virtual mouse button, execute `body`, then release the virtual mouse button."
  [driver & body]
  `(do
     (mouse-btn-down ~driver)
     (try
       ~@body
       (finally
         (mouse-btn-up ~driver)))))

(defmulti drag-and-drop
  "Have `driver` perform a drag and drop from element found by `q-from` to element found by `q-to`:

  1. moves mouse pointer to an element found with `q-from` query;
  2. holds down the mouse button;
  3. moves mouse to an element found with `q-to` query;
  4. releases the mouse button.

  See [[query]] for details on `q-from`, `q-to`.

  Notes:
  - does not work in Phantom.js since it does not have a virtual mouse API;
  - does not work in Safari."
  {:arglists '([driver q-from q-to])}
  dispatch-driver)

(defmethod drag-and-drop
  :phantom
  [driver q-from q-to]
  (mouse-move-to driver q-from)
  (with-mouse-btn driver
    (mouse-move-to driver q-to)))

(defmethod drag-and-drop
  :default
  [driver q-from q-to]
  (let [el-from (query driver q-from)
        el-to   (query driver q-to)
        mouse   (-> (make-mouse-input)
                    (add-pointer-move-to-el el-from)
                    (with-pointer-left-btn-down
                      (add-pointer-move-to-el el-to)))]
    (perform-actions driver mouse)))

;;
;; Clicking
;;

(defn click-el
  "Have `driver` click on element `el`."
  [driver el]
  {:pre [(some? el)]}
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :element el :click]}))

(defn click
  "Have `driver` click on element found by query `q`.

  See [[query]] for details on `q`."
  [driver q]
  (click-el driver (query driver q)))


(defn click-single
  "Have `driver` click on element found by query `q`.
  If `q` returns more than one element, throws.

  See [[query]] for details on `q`."
  [driver q]
  (let [elements (query-all driver q)]
    (if (> (count elements) 1)
      (throw (Exception.
               (format "Multiple elements found: %s, query %s"
                       (count elements) q)))
      (click-el driver (first elements)))))

(defn click-multi
  "Have `driver` click on first element found by each query in vector `qs`.

  - `qs`  a vector of queries `[query1 query2 query3 ...]`
  - `pause` a pause prior to each click in seconds, default is `0`.

  See [[query]] for details on `q`s."
  [driver qs & [pause]]
  (doseq [q qs]
    (wait (or pause 0))
    (click driver q)))

;; Double click

(defmulti double-click-el
  "Have `driver` double-click on element `el`.

  Note:
  - the supported browsers are Chrome, and Phantom.js.
  - for Firefox and Safari, your may try to simulate it as a `click, wait, click` sequence."
  {:arglists '([driver el])}
  dispatch-driver)

(defmethod double-click-el
  :default
  [driver el]
  {:pre [(some? el)]}
  (let [mouse   (-> (make-mouse-input)
                    (add-pointer-move-to-el el)
                    add-pointer-click
                    (add-pause 10)
                    add-pointer-click)]
    (perform-actions driver mouse)))

(defn double-click
  "Have `driver` double-click on element found by query `q`.

  See [[query]] for details on `q`.

  Note:
  - the supported browsers are Chrome, and Phantom.js.
  - for Firefox and Safari, your may try to simulate via `click, wait, click` sequence."
  [driver q]
  (double-click-el driver (query driver q)))


;; Blind click

(defmulti mouse-click
  "Have `driver` click `mouse-button` at current mouse pointer position.

  `mouse-button` should be `etaoin.keys/mouse-left`,`etaoin.keys/mouse-middle` or `etaoin.keys/mouse-right`."
  {:arglists '([driver mouse-button])}
  dispatch-driver)

(defmethod mouse-click
  :default
  [driver mouse-button]
  (let [{driver-type :type} driver]
    (throw (ex-info "Mouse click is not supported for that browser"
                    {:button      mouse-button
                     :driver-type driver-type}))))

(defmethod mouse-click
  :chrome ;; TODO: try safari once the issue with it is solved
  [driver mouse-button]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :click]
            :data   {:button mouse-button}}))

(defn left-click
  "Have `driver` click the left mouse button at current mouse pointer position."
  [driver]
  (mouse-click driver k/mouse-left))

(defn right-click
  "Have `driver` click the right mouse button at current mouse pointer position."
  [driver]
  (mouse-click driver k/mouse-right))

(defn middle-click
  "Have `driver` click the middle mouse button at current mouse pointer position."
  [driver]
  (mouse-click driver k/mouse-middle))

(defn mouse-click-on
  "Have `driver` move mouse pointer to element found by `q` then click `mouse-button`.

  - `mouse-button` should be `etaoin.keys/mouse-left`,`etaoin.keys/mouse-middle` or `etaoin.keys/mouse-right`.
  - `q` see [[query]] for details."
  [driver btn q]
  (doto driver
    (mouse-move-to q)
    (mouse-click btn)))

(defn left-click-on
  "Have `driver` move mouse pointer to element found by `q` then click left mouse button.

  See [[query]] for details on `q`."
  [driver q]
  (mouse-click-on driver k/mouse-left q))

(defn right-click-on
  "Have `driver` move mouse pointer to element found by `q` then click right mouse button.

  See [[query]] for details on `q`."
  [driver q]
  (mouse-click-on driver k/mouse-right q))

(defn middle-click-on
  "Have `driver` move mouse pointer to element found by `q` then click middle mouse button.

  See [[query]] for details on `q`.

  Useful for opening links in a new tab."
  [driver q]
  (mouse-click-on driver k/mouse-middle q))

;;
;; Element size
;;

(defmulti get-element-size-el
  "Have `driver` return map of `:width` and `:height`, in pixels, of element `el`."
  {:arglists '([driver el])}
  dispatch-driver)

(defmethods get-element-size-el
  [:chrome :edge :phantom]
  [driver el]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :element el :size]})
      :value
      (select-keys [:width :height])))

(defmethods get-element-size-el
  [:firefox :safari]
  [driver el]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :element el :rect]})
      :value
      (select-keys [:width :height])))

(defn get-element-size
  "Have `driver` return map of `:width` and `:height`, in pixels, of element found by query `q`.

  See [[query]] for details on `q`."
  [driver q]
  (get-element-size-el driver (query driver q)))

;;
;; element location
;;

(defmulti get-element-location-el
  "Have `driver` return map of `:x` `:y` offset, in pixels of element `el`."
  {:arglists '([driver el])}
  dispatch-driver)

(defmethods get-element-location-el
  [:chrome :edge :phantom]
  [driver el]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :element el :location]})
      :value
      (select-keys [:x :y])))

(defmethods get-element-location-el
  [:firefox :safari]
  [driver el]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :element el :rect]})
      :value
      (select-keys [:x :y])))

(defn get-element-location
  "Have `driver` return map of `:x` `:y` offset, in pixels of element found by query `q`.

  See [[query]] for details on `q`."
  [driver q]
  (get-element-location-el driver (query driver q)))

;;
;; element box
;;

(defn get-element-box
  "Have `driver` return map describing a bounding box for element found by query `q`.

  See [[query]] for details on `q`.

  The result is a map with the following keys:

  - `:x1`: top left `x` coordinate;
  - `:y1`: top left `y` coordinate;
  - `:x2`: bottom right `x` coordinate;
  - `:y2`: bottom right `y` coordinate;
  - `:width`: width as a difference b/w `:x2` and `:x1`;
  - `:height`: height as a difference b/w `:y2` and `:y1`. "
  [driver q]
  (let [el                     (query driver q)
        {:keys [width height]} (get-element-size-el driver el)
        {:keys [x y]}          (get-element-location-el driver el)]
    {:x1     x
     :x2     (+ x width)
     :y1     y
     :y2     (+ y height)
     :width  width
     :height height}))

(defn intersects?
  "Return true if bounding boxes found by `driver` for element found by query `q1`
  intersects element found by query `q2`.

  Compares bounding boxes of found elements."
  [driver q1 q2]
  (let [a (get-element-box driver q1)
        b (get-element-box driver q2)]
    (or (< (a :y1) (b :y2))
        (> (a :y2) (b :y1))
        (< (a :x2) (b :x1))
        (> (a :x1) (b :x2)))))

;;
;; properties
;;

(defn get-element-property-el
  "Have `driver` return value for `property` of element `el`."
  [driver el property]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :property (name property)]})))

(defn get-element-property
  "Have `driver` value for  `property` of element found by query `q`.

  Found property value is returned as string.
  When element is found but property is absent, returns `nil`.

  See [[query]] for details on `q`."
  [driver q property]
  (get-element-property-el driver (query driver q) property))

(defn get-element-properties
  "Have `driver` return a vector of property values matching `properties` of element found by query `q`.

  Found property values are returned as strings.
  When element is found but property is absent, result is included in vector as `nil`.

  See [[query]] for details on `q`."
  [driver q & properties]
  (let [el (query driver q)]
    (vec
      (for [prop properties]
        (get-element-property-el driver el prop)))))

;;
;; attributes
;;

(defn get-element-attr-el
  "Have `driver` return value for `attribute` of element `el`, or `nil` if attribute does not exist."
  [driver el attribute]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :attribute (name attribute)]})))

(defn get-element-attr
  "Have `driver` return value for `attribute` of element found by `q`.

  Found attribute value is returned as string.
  When element is found but attribute is absent, returns `nil`.

  See [[query]] for details on `q`.

  Note: there is no special treatment of the `class` attribute.
  A single string with spaces is returned.

  Example:

  ```Clojure
  (def driver (firefox))
  (get-element-attr driver {:tag :a} :class)
  ;; => \"link link__external link__button\"
  ```"
  [driver q attribute]
  (get-element-attr-el driver (query driver q) attribute))

(defn get-element-attrs
  "Have `driver` return values for `attributes` of element found by query `q`.

  Found attribute values are returned as strings.
  When element is found but attribute is absent, result is included in vector as `nil`.

  See [[query]] for details on `q`."
  [driver q & attributes]
  (let [el (query driver q)]
    (vec
      (for [attr attributes]
        (get-element-attr-el driver el attr)))))

;;
;; css
;;

(defn get-element-css-el
  "Have `driver` return value for CSS style `property` of element `el`, or `nil` if property does not exist."
  [driver el property]
  {:pre [(some? el)]}
  (-> (execute {:driver driver
                :method :get
                :path   [:session (:session driver) :element el :css (name property)]})
      :value
      not-empty))

(defn get-element-css
  "Have `driver` return the CSS style `property` of element found by query `q`.

  `property` is a string/keyword representing a CSS name.
  Examples: `:font` or `\"background-color\"`

  The property will be returned if it was defined for the element itself or inherited.

  Found property value is returned as string.
  When element is found but property is absent, returns `nil`.

  See [[query]] for details on `q`.

  Note: colors, fonts and some other properties may be represented differently for different browsers.

  Example:
  ```Clojure
  (def driver (firefox))
  (e/go driver \"https://clojars.org\")
  (get-element-css driver {:id :content} :background-color)
  ;; => \"rgb(226, 228, 227)\"
  ```"
  [driver q property]
  (get-element-css-el driver (query driver q) property))


(defn get-element-csss
  "Have `driver` return CSS style properties matching `properties` of element found by query `q`.

  See [[query]] for details on `q`.

  Found property values are returned as strings.
  When element is found but property is absent, result is included in vector as `nil`.

  Note: colors, fonts and some other properties may be represented differently for different browsers.

  Example:
  ```Clojure
  (def driver (firefox))
  (e/go driver \"https://clojars.org\")
  (e/get-element-csss driver {:tag :body} :background-color :typo :line-height :font-size)
  ;; => [\"rgb(226, 228, 227)\" nil \"20px\" \"14px\"]
  ```"
  [driver q & properties]
  (let [el (query driver q)]
    (vec
      (for [prop properties]
        (get-element-css-el driver el prop)))))

;;
;; element inner HTML
;;
(defmulti get-element-inner-html-el
  "Have `driver` return inner text of element `el`.

  For element with `my-id` in `<div id=\"my-id\"><p class=\"foo\">hello</p></div>` return will be
  `\"<p class=\"foo\">hello</p>\"`."
  {:arglists '([driver el])}
  dispatch-driver)

(defmethod get-element-inner-html-el
  :default
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :property :innerHTML]})))

(defmethod get-element-inner-html-el
  :phantom
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :attribute :innerHTML]})))

(defn get-element-inner-html
  "Have `driver` return inner text of element found by query `q`.

  See [[query]] for details on `q`.

  For element with `my-id` in `<div id=\"my-id\"><p class=\"foo\">hello</p></div>` return will be
  `\"<p class=\"foo\">hello</p>\"`."
  [driver q]
  (get-element-inner-html-el driver (query driver q)))

;;
;; element text, name and value
;;

(defn get-element-tag-el
  "Have `driver` return tag name of element `el`."
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :name]})))

(defn get-element-tag
  "Have `driver` return tag name of element found by query `q`.

  See [[query]] for details on `q`."
  [driver q]
  (get-element-tag-el driver (query driver q)))

(defn get-element-text-el
  "Have `driver` return text of element `el`.

  Text return for `<p class=\"foo\">hello</p>` is  `\"hello\"`."
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :text]})))

(defn get-element-text
  "Have `driver` return inner text of element found by query `q`.

  See [[query]] for details on `q`.

  Text return for `<p class=\"foo\">hello</p>` is  `\"hello\"`."
  [driver q]
  (get-element-text-el driver (query driver q)))

;;
;; Element value
;;

(defn get-element-value-el
  "Have `driver` return the value of element `el`.

  To be used on input elements."
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :value]})))

(defmulti get-element-value
  "Have `driver` return the value of element found by query `q`.

  To be used on input elements.

  See [[query]] for details on `q`."
  {:arglists '([driver q])}
  dispatch-driver)

(defmethod get-element-value
  :default
  [driver q]
  (get-element-value-el driver (query driver q)))

(defmethods get-element-value
  [:phantom]
  [driver q]
  (get-element-attr driver q :value))

(defmethods get-element-value
  [:firefox :safari]
  [driver q]
  (get-element-property driver q :value))

(defn get-element-shadow-root-el
  "Returns the shadow root for the specified element or `nil` if the
  element does not have a shadow root."
  [driver el]
  (-> (get-element-property-el driver el "shadowRoot")
      first
      second))

(defn get-element-shadow-root
  "Returns the shadow root for the first element matching the query, or
  `nil` if the element does not have a shadow root.

  See [[query]] for more details on `q`."
  [driver q]
  (get-element-shadow-root-el driver (query driver q)))

;;; 
;;; Shadow root queries
;;; 

(defmulti ^:private find-element-from-shadow-root* dispatch-driver)

(defmethods find-element-from-shadow-root*
  [:firefox :safari]
  [driver shadow-root-el locator term]
  {:pre [(some? shadow-root-el)]}
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :shadow shadow-root-el :element]
                :data   {:using locator :value term}})
      :value
      first
      second))

(defmethod find-element-from-shadow-root* :default
  [driver shadow-root-el locator term]
  {:pre [(some? shadow-root-el)]}
  (-> (execute {:driver driver
                :method :post
                :path   [:session (:session driver) :shadow shadow-root-el :element]
                :data   {:using locator :value term}})
      :value
      :ELEMENT))

(defmulti ^:private find-elements-from-shadow-root* dispatch-driver)

(defmethod find-elements-from-shadow-root* :default
  [driver shadow-root-el locator term]
  {:pre [(some? shadow-root-el)]}
  (->> (execute {:driver driver
                 :method :post
                 :path   [:session (:session driver) :shadow shadow-root-el :elements]
                 :data   {:using locator :value term}})
       :value
       (mapv (comp second first))))

(defn query-shadow-root-el
  "Queries the shadow DOM rooted at `shadow-root-el`, looking for the
  first element specified by `shadow-q`.

  The `shadow-q` parameter is similar to the `q` parameter of
  the [[query]] function, but some drivers may limit it to specific
  formats (e.g., CSS). See [this note](/doc/01-user-guide.adoc#shadow-root-browser-limitations) for more information."
  [driver shadow-root-el shadow-q]
  (let [[loc term] (query/expand driver shadow-q)]
    (find-element-from-shadow-root* driver shadow-root-el loc term)))

(defn query-all-shadow-root-el
  "Queries the shadow DOM rooted at `shadow-root-el`, looking for all
  elements specified by `shadow-q`.

  The `shadow-q` parameter is similar to the `q` parameter of
  the [[query]] function, but some drivers may limit it to specific
  formats (e.g., CSS). See [this note](/doc/01-user-guide.adoc#shadow-root-browser-limitations) for more information."
  [driver shadow-root-el shadow-q]
  (let [[loc term] (query/expand driver shadow-q)]
    (find-elements-from-shadow-root* driver shadow-root-el loc term)))

(defn query-shadow-root
  "First, conducts a standard search (as if by [[query]]) for an element
  with a shadow root. Then, from that shadow root element, conducts a
  search of the shadow DOM for the first element matching `shadow-q`.

  For details on `q`, see [[query]].

  The `shadow-q` parameter is similar to the `q` parameter of
  the [[query]] function, but some drivers may limit it to specific
  formats (e.g., CSS). See [this note](/doc/01-user-guide.adoc#shadow-root-browser-limitations) for more information."
  [driver q shadow-q]
  (query-shadow-root-el driver (get-element-shadow-root driver q) shadow-q))

(defn query-all-shadow-root
  "First, conducts a standard search (as if by [[query]]) for an element
  with a shadow root. Then, from that shadow root element, conducts a
  search of the shadow DOM for all elements matching `shadow-q`.

  For details on `q`, see [[query]].

  The `shadow-q` parameter is similar to the `q` parameter of
  the [[query]] function, but some drivers may limit it to specific
  formats (e.g., CSS). See [this note](/doc/01-user-guide.adoc#shadow-root-browser-limitations) for more information."
  [driver q shadow-q]
  (query-all-shadow-root-el driver (get-element-shadow-root driver q) shadow-q))

;;
;; cookies
;;

(defn get-cookies
  "Have `driver` return a vector of current browser cookies.

  Each cookie is a map with structure:

  ```Clojure
  {:name \"cookie1\",
   :value \"test1\",
   :path \"/\",
   :domain \"\",
   :expiry nil,
   :secure false,
   :httpOnly false}
  ```"
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :cookie]})))

(defn get-cookie
  "Have `driver` return first cookie matching `cookie-name`.

  When `cookie-name` is a keyword it will be converted appropriately."
  [driver cookie-name]
  (->> driver
       get-cookies
       (filter #(= (:name %) (name cookie-name)))
       first))

(defn set-cookie
  "Have `driver` set a `cookie`.

  `cookie` is a map with structure described in [[get-cookies]].
  At least `:name` and `:value` keys should be populated."
  [driver cookie]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :cookie]
            :data   {:cookie cookie}}))

(defn delete-cookie
  "Have `driver` delete cookie with cookie `cookie-name`."
  [driver cookie-name]
  (execute {:driver driver
            :method :delete
            :path   [:session (:session driver) :cookie (name cookie-name)]}))

(defmulti delete-cookies
  "Have `driver` delete all browser cookies for all domains."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod delete-cookies :default
  [driver]
  (execute {:driver driver
            :method :delete
            :path   [:session (:session driver) :cookie]}))

;; TODO test & delete
(defmethod delete-cookies :safari
  ;; For some reason, Safari hangs forever when trying to delete
  ;; all cookies. Currently we delete them in cycle.
  [driver]
  (doseq [cookie (get-cookies driver)]
    (delete-cookie driver (:name cookie))))

;;
;; source code
;;

(defn get-source
  "Have `driver` return browser's current page HTML markup as a string."
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :source]})))

;;
;; Javascript
;;

(defn el->ref
  "Return map representing an element reference for WebDriver.

  The magic `:element-` constant in source is taken from the [WebDriver Spec](https://www.w3.org/TR/webdriver/#elements).

  Passing the element reference map to `js-execute` automatically expands it
  into a DOM node. For example:

  ```Clojure
  ;; returns UUID string for the element
  (def el (query driver :button-ok))

  ;; the first argument will the an Element instance.
  (js-execute driver \"arguments[0].scrollIntoView()\", (el->ref el))
  ```"
  [el]
  {:ELEMENT                             el
   :element-6066-11e4-a52e-4f735466cecf el})

(defmulti js-execute
  "Return result of `driver` executing Javascript `script` with `args` synchronously in the browser.

  The script is sent as a string (can be multi-line). Under the hood,
  the browser wraps your code into a function, so avoid using the `function`
  clause at the top level.

  Don't forget to add `return <something>` operator if you are
  interested in a resulting value.

  You may access arguments through the built-in `arguments`
  pseudo-array from your code. You may pass any data structures that
  are JSON-compatible (scalars, maps, vectors).

  The result value is also returned trough JSON encode/decode
  pipeline (JS objects turn to Clojure maps, arrays into vectors and
  so on).

  - `args`: additional arguments for your script. Automatically converted to JSON.

  Example:
  ```Clojure
  (def driver (chrome))
  (js-execute driver \"return arguments[0] + 1;\" 42)
  ;; => 43
  ```"
  {:arglists '([driver script & args])}
  dispatch-driver)

(defmethod js-execute :default
  [driver script & args]
  (:value (execute {:driver driver
                    :method :post
                    :path   [:session (:session driver) :execute]
                    :data   {:script script :args (vec args)}})))

(defmethods js-execute
  [:firefox :safari]
  [driver script & args]
  (:value (execute {:driver driver
                    :method :post
                    :path   [:session (:session driver) :execute :sync]
                    :data   {:script script :args (vec args)}})))

(defmulti js-async
  "Return result of `driver` executing JavaScript `script` with `args` asynchornously in the browser.

  Executes an asynchronous script in the browser and returns the result.
  An asynchronous script one that typically performs some kind of IO operation,
  like an AJAX request to the server. You cannot just use the `return` statement
  like you do in synchronous scripts.

  The driver passes a special handler as the last argument that should be called
  to return the final result.

  *Note:* calling this function requires the `script` timeout to be set properly,
  meaning non-zero positive value. See [[get-script-timeout]], [[set-script-timeout]]
  and [[with-script-timeout]].

  - `args`: additional arguments for your code. Automatically converted to JSON.

  Example of a script:

  ```Clojure
  // the `arguments` would be an array of something like:
  // [1, 2, true, ..., <special callback added by driver>]

  var callback = arguments[arguments.length-1];

  // so the main script would look like:
  $.ajax({url: '/some/url', success: function(result) {
    if (isResultOk(result)) {
      callback({ok: getProgressData(result)});
    }
    else {
      callback({error: getErrorData(result)});
    }
  }});
  ```"
  {:arglists '([driver script & args])}
  dispatch-driver)

(defmethod js-async :default
  [driver script & args]
  (:value (execute {:driver driver
                    :method :post
                    :path   [:session (:session driver) :execute_async]
                    :data   {:script script :args (vec args)}})))

(defmethod js-async
  [:firefox :safari]
  [driver script & args]
  (:value (execute {:driver driver
                    :method :post
                    :path   [:session (:session driver) :execute :async]
                    :data   {:script script :args (vec args)}})))

;;
;; User-Agent
;;

(defn get-user-agent
  "Have `driver` return the browser `User-Agent`"
  [driver]
  (js-execute driver "return navigator.userAgent"))


;;
;; Javascript helpers
;;

(defn js-localstorage-clear
  "Have `driver` clear local storage."
  [driver]
  (js-execute driver "localStorage.clear()"))

(defn add-script
  "Have `driver` add script with src `url` to page."
  [driver url]
  (let [script
        (str "var s = document.createElement('script');"
             "s.type = 'text/javascript';"
             "s.src = arguments[0];"
             "document.head.appendChild(s);")]
    (js-execute driver script url)))

;;
;; scrolling
;;

(defn scroll
  "Have `driver` scroll page to `x` `y` absolute pixel coordinates."
  ([driver x y]
   (js-execute driver "window.scroll(arguments[0], arguments[1]);" x y))
  ([driver {:keys [x y]}]
   (scroll driver x y)))

(defn scroll-by
  "Have `driver` scroll by `x` `y` relative pixel offset."
  ([driver x y]
   (js-execute driver "window.scrollBy(arguments[0], arguments[1]);" x y))
  ([driver {:keys [x y]}]
   (scroll-by driver x y)))

(defn scroll-query
  "Have `driver` scroll to the element found by query `q`.

  See [[query]] for details on `q`.

  Invokes element's `.scrollIntoView()` method. Accepts extra `param`
  argument that might be either boolean or object for more control.
  See [Mozilla's docs for values](https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView)."
  ([driver q]
   (let [el (query driver q)]
     (js-execute driver "arguments[0].scrollIntoView();" (el->ref el))))
  ([driver q param]
   (let [el (query driver q)]
     (js-execute driver "arguments[0].scrollIntoView(arguments[1]);" (el->ref el) param))))

(defn get-scroll
  "Have `driver` return the current scroll position as a map of `:x` `:y`."
  [driver]
  (js-execute driver "return {x: window.scrollX, y: window.scrollY};"))

(defn scroll-top
  "Have `driver` scroll vertically to the top of the page keeping current horizontal position."
  [driver]
  (let [{:keys [x _y]} (get-scroll driver)]
    (scroll driver x 0)))

(defn scroll-bottom
  "Have `driver` scroll vertically to bottom of the page keeping current horizontal position."
  [driver]
  (let [y-max         (js-execute driver "return document.body.scrollHeight;")
        {:keys [x _y]} (get-scroll driver)]
    (scroll driver x y-max)))

(def ^{:doc "Default scroll offset in pixels."}
  scroll-offset 100)

(defn scroll-up
  "Have `driver` scroll the page up by `offset` pixels.

  `offset` defaults to [[scroll-offset]]."
  ([driver offset]
   (scroll-by driver 0 (- offset)))
  ([driver]
   (scroll-up driver scroll-offset)))

(defn scroll-down
  "Have `driver` scroll the page down by `offset` pixels.

  `offset` defaults to [[scroll-offset]]."
  ([driver offset]
   (scroll-by driver 0 offset))
  ([driver]
   (scroll-down driver scroll-offset)))

(defn scroll-left
  "Have `driver` scroll the page left by `offset` pixels.

  `offset` defaults to [[scroll-offset]]."
  ([driver offset]
   (scroll-by driver (- offset) 0))
  ([driver]
   (scroll-left driver scroll-offset)))

(defn scroll-right
  "Have `driver` scroll the page right by `offset` pixels.

  `offset` defaults to [[scroll-offset]]."
  ([driver offset]
   (scroll-by driver offset 0))
  ([driver]
   (scroll-right driver scroll-offset)))

;;
;; iframes
;;

(defn ^:no-doc switch-frame*
  "Have `driver` switch context to (i)frame by its `id`

  `id` can be an index or the element `id` attribute."
  [driver id]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :frame]
            :data   {:id id}}))

(defn switch-frame
  "Have `driver` switch context to (i)frame element found by query `q`.

  See [[query]] for details on `q`."
  [driver q]
  (let [el (query driver q)]
    (switch-frame* driver (el->ref el))))

(defn switch-frame-first
  "Have `driver` switch context to the first (i)frame."
  [driver]
  (switch-frame* driver 0))

(defn switch-frame-parent
  "Have `driver` switch context to the parent of the current (i)frame."
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :frame :parent]}))

(defn switch-frame-top
  "Have `driver` switch context the main page."
  [driver]
  (switch-frame* driver nil))

(defmacro with-frame
  "Excecute `body` within context of frame found via `driver` by query `q`.

  Frame context is restored after call.
  Result of `body` is returned."
  [driver q & body]
  `(do
     (switch-frame ~driver ~q)
     (let [result# (do ~@body)]
       (switch-frame-parent ~driver)
       result#)))

;;
;; logs
;;

(defmulti get-log-types
  "Have `driver` return a vector of log types the browser supports."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-log-types
  [:chrome :phantom]
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :log :types]})))

(defn ^:no-doc process-log
  "Remaps some of the log's fields."
  [entry]
  (-> entry
      (update :level (comp keyword str/lower-case))
      (update :source keyword)
      (assoc :datetime (java.util.Date. ^long (:timestamp entry)))))

(defmulti ^:private get-logs*
  {:arglists '([driver logtype])}
  dispatch-driver)

(defmethods get-logs*
  [:chrome :phantom]
  [driver logtype]
  (->> (execute {:driver driver
                 :method :post
                 :path   [:session (:session driver) :log]
                 :data   {:type logtype}})
       :value
       (mapv process-log)))

(defn get-logs
  "Have `driver` return Javascript console log entries.

  Each log entry is a map with the following structure:
  ```Clojure
  {:level :warning,
   :message \"1,2,3,4  anonymous (:1)\",
   :timestamp 1511449388366,
   :source nil,
   :datetime #inst \"2017-11-23T15:03:08.366-00:00\"}
  ```

  Empirical knowledge about browser differences:

  - Chrome:
    - Returns all recorded logs.
    - Clears the logs once they have been read.
    - JS console logs have `:console-api` for `:source` field.
    - Entries about errors will have SEVERE level.

  - PhantomJS (obsolete and no longer tested):
    - Return all recorded logs since the last URL change.
    - Does not clear recorded logs on subsequent invocations.
    - JS console logs have nil for `:source` field.
    - Entries about errors will have WARNING level, as coded [here](https://github.com/detro/ghostdriver/blob/be7ffd9d47c1e76c7bfa1d47cdcde9164fd40db8/src/session.js#L494)."
  ([driver]
   (get-logs driver "browser"))
  ([driver logtype]
   (get-logs* driver logtype)))

(defn supports-logs?
  "Returns true if `driver` supports getting console logs."
  [driver]
  (implemented? driver get-logs*))

(defn- dump-logs
  [logs filename & [opts]]
  (json/generate-stream
    logs
    (io/writer filename)
    (merge {:pretty true} opts)))

;;
;; get/set hash
;;

(defn- split-hash [url]
  (str/split url #"#" 2))

(defn set-hash
  "Have `driver` set a new `hash` fragment for the current page.

  Don't include the leading `#` character in `hash`.

  Useful when navigating on single page applications. See also: [[get-hash]]."
  [driver hash]
  (let [[url _] (split-hash (get-url driver))
        new     (format "%s#%s" url hash)]
    (go driver new)))

(defn get-hash
  "Have `driver` fetch the current hash fragment for the current page (nil when not set).

  Example:
  ```Clojure
  (def driver (chrome))
  (go driver \"https://en.wikipedia.org/wiki/Clojure\")
  (get-hash driver)
  ;; => nil
  (go driver \"https://en.wikipedia.org/wiki/Clojure#Popularity\")
  (get-hash driver)
  ;; => \"Popularity\"
  ````

  See also: [[set-hash]]."
  [driver]
  (let [[_ hash] (split-hash (get-url driver))]
    hash))
;;
;; exceptions
;;

(defmacro ^:no-doc with-exception [catch fallback & body]
  `(try+
     ~@body
     (catch ~catch ~(quote _)
       ~fallback)))

(defmacro with-http-error
  "Executes `body` suppressing catching any exception that would normally occur due to HTTP non-success status
  when communicating with a WebDriver.

  Instead returns false on first HTTP non-success status."
  [& body]
  `(with-exception [:type :etaoin/http-error] false
     ~@body))

;;
;; default locators
;;

(defn- use-locator
  "Return `driver` with new default `locator`"
  [driver locator]
  (assoc driver :locator locator))

(defn use-xpath
  "Return new `driver` with default locator set to XPath."
  [driver]
  (use-locator driver locator-xpath))

(defn use-css
  "Return new `driver` with default locator set to CSS."
  [driver]
  (use-locator driver locator-css))

(defmacro ^:no-doc with-locator [driver locator & body]
  `(binding [~driver (assoc ~driver :locator ~locator)]
     ~@body))

(defmacro with-xpath
  "Execute `body` with default locator set to XPath."
  [driver & body]
  `(with-locator ~driver locator-xpath
     ~@body))

(defmacro with-css
  "Execute `body` with default locator set to CSS."
  [driver & body]
  `(with-locator ~driver locator-css
     ~@body))

;;
;; alerts
;;

(defmulti get-alert-text
  "Have `driver` return text string from alert dialog (if present)."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-alert-text
  [:firefox :safari]
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :alert :text]})))

(defmethods get-alert-text
  [:chrome :edge]
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :alert_text]})))

(defmulti dismiss-alert
  "Have `driver` cancel open alert dialog."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods dismiss-alert
  [:firefox :safari]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :alert :dismiss]}))

(defmethods dismiss-alert
  [:chrome :edge]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :dismiss_alert]}))

(defmulti accept-alert
  "Have `driver` accept open alert dialog."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods accept-alert
  [:firefox :safari]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :alert :accept]}))

(defmethods accept-alert
  [:chrome :edge]
  [driver]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :accept_alert]}))

;;
;; network
;;

(defn running?
  "Return true if `driver` seems accessable via its `:host` and `:port`.

  Throws
  - if using `:webdriver-url`
  - if driver process is found not be running"
  [driver]
  (when (:webdriver-url driver)
    (throw (ex-info "Not supported for driver using :webdriver-url" {})))

  (when-let [process (:process driver)]
    (when-not (proc/alive? process)
      (throw (ex-info (format "WebDriver process exited unexpectedly with a value: %d"
                              (:exit (proc/result process)))
                      {}))))
  (util/connectable? (:host driver)
                     (:port driver)))



;;
;; predicates
;;

(defn driver?
  "Return true if `driver` is of `type` (e.g. on of: `:chrome`, `:edge`, `:firefox`, `:safari`, `:phantom`)"
  [driver type]
  (= (dispatch-driver driver) type))

(defn chrome?
  "Returns true if a `driver` is Chrome."
  [driver]
  (driver? driver :chrome))

(defn edge?
  "Returns true if a `driver` is Edge."
  [driver]
  (driver? driver :edge))

(defn firefox?
  "Returns true if a `driver` is Firefox."
  [driver]
  (driver? driver :firefox))

(defn phantom?
  "Returns true if a `driver` is Phantom.js."
  [driver]
  (driver? driver :phantom))

(defn safari?
  "Returns true if a `driver` is Safari."
  [driver]
  (driver? driver :safari))

(defn headless?
  "Returns true if a `driver` is in headless mode (without UI window)."
  [driver]
  (drv/is-headless? driver))

(defn exists?
  "Return true if `driver` can find element via query `q`.

  See [[query]] for details on `q`.

  Keep in mind this does not validate whether the found element is visible, clickable and so on."
  [driver q & more]
  (with-http-error
    (apply query driver q more)
    true))

(def ^{:doc "Opposite of [[exists?]]."
       :arglists '([driver q & more])}
  absent? (complement exists?))

(defmulti displayed-el?
  "Return true if `driver` finds `el` is displayed/visible.

  See [[query]] for details on `q`.

  Note: Safari webdriver has not implemented `displayed`, for it
  we currently default to some naive CSS display/visibilty checks."
  {:arglists '([driver el])}
  dispatch-driver)

(defmethod displayed-el? :default
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :displayed]})))

(defmethod displayed-el? :safari
  [driver el]
  {:pre [(some? el)]}
  (cond
    (= (get-element-css-el driver el :display)
       "none")
    false
    (= (get-element-css-el driver el :visibility)
       "hidden")
    false
    :else true))

(defn displayed?
  "Return true if element found by `driver` with query `q` is effectively displayed."
  [driver q]
  (displayed-el? driver (query driver q)))

(defn visible?
  "Return true if element found by `driver` with query `q` exists and is effectively displayed.

  See [[query]] for details on `q`.

  Same as [[displayed?]] but does not throw if element does not exist."
  [driver q]
  (and (exists? driver q)
       (displayed? driver q)))

(def ^{:doc "Oppsite of  [[visible?]]."
       :arglists '([driver q])}
  invisible? (complement visible?))

(defn selected-el?
  "Return true if `driver` determines element `el` is selected.

  For use on input elements like checkboxes, radio buttons and option elements."
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :selected]})))

(defn selected?
  "Return true if `driver` determines element found by query `q` is selected.

  See [[query]] for details on `q`.

  For use on input elements like checkboxes, radio buttons and option elements."
  [driver q]
  (selected-el? driver (query driver q)))

(defn enabled-el?
  "Return true if `driver` determines element `el` is enabled.

  For use on form elements."
  [driver el]
  {:pre [(some? el)]}
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :element el :enabled]})))

(defn enabled?
  "Returns true if `driver` determines element found by query `q` is enabled.

  See [[query]] for details on `q`.

  For use on form elements."
  [driver q]
  (enabled-el? driver (query driver q)))

(def ^{:doc "Opposite of [[enabled?]]"
       :arglists '([query q])}
  disabled?  (complement enabled?))

(defn has-text?
  "Return true if `driver` finds that `text` appears anywhere on a page.

  When `q` is specified, restricts search inside the element that matches query `q`.
  See [[query]] for details on `q`."
  ([driver text]
   (with-http-error
     (boolean
       (query driver {:xpath (xpath/node-by-text text)}))))

  ([driver q text]
   (let [x {:xpath (xpath/node-by-text text)}]
     (with-http-error
       (boolean
         (if (vector? q)
           (apply query driver (conj q x))
           (query driver q x)))))))

(defn has-class-el?
  "Returns true if `driver` finds that element `el` includes `class` in its class attribute."
  [driver el class]
  {:pre [(some? el)]}
  (let [classes (get-element-attr-el driver el "class")]
    (cond
      (nil? classes) false
      (string? classes)
      (str/includes? classes (name class)))))

(defn has-class?
  "Returns true if `driver` finds that element found by query `q` includes `class` in its class attribute.

  See [[query]] for details on `q`."
  [driver q class]
  (has-class-el? driver (query driver q) class))

(def ^{:doc "Opposite of [[has-class?]]."
       :arglists '([query q class])}
  has-no-class? (complement has-class?))

(defn has-alert?
  "Returns true if `driver` sees an open alert dialog."
  [driver]
  (with-http-error
    (get-alert-text driver)
    true))

(def ^{:doc "Opposite of [[has-alert?]]."
       :arglists '([driver])}
  has-no-alert? (complement has-alert?))

(defn has-shadow-root-el?
  "Returns `true` if the specified element has a shadow root or `false` otherwise."
  [driver el]
  (boolean (get-element-shadow-root-el driver el)))

(defn has-shadow-root?
  "Returns `true` if the first element matching the query has a shadow
  root or `false` otherwise."
  [driver q]
  (boolean (get-element-shadow-root driver q)))

;;
;; wait functions
;;

(def ^:dynamic *wait-timeout* "Maximum seconds to wait, default for `wait-*` functions." 7)
(def ^:dynamic *wait-interval* "Frequency in seconds to check if we should still wait, default for `wait-*` functions." 0.33)

(defmacro with-wait-timeout
  "Execute `body` with a [[*wait-timeout*]] of `seconds`"
  [seconds & body]
  `(binding [*wait-timeout* ~seconds]
     ~@body))

(defmacro with-wait-interval
  "Execute `body` with a [[*wait-interval*]] of `seconds` (which can be fractional)."
  [seconds & body]
  `(binding [*wait-interval* ~seconds]
     ~@body))

(defn wait
  "Sleep for `seconds`."
  (#_{:clj-kondo/ignore [:unused-binding]} [driver seconds]
   (wait seconds))
  ([seconds]
   (Thread/sleep (long (* seconds 1000)))))

(defmacro with-wait
  "Execute `body` waiting `seconds` before each form.

  Returns the value of the last form.

  Can be used to perform actions slowly. Some SPA applications need extra time
  to re-render the content."
  [seconds & body]
  `(do ~@(interleave (repeat `(wait ~seconds)) body)))

(defmacro doto-wait
  "The same as `clojure.core/doto` but prepends each form with ([[wait]] `seconds`) clause."
  [seconds obj & body]
  `(doto ~obj
     ~@(interleave (repeat `(wait ~seconds)) body)))

(defn wait-predicate
  "Wakes up every `:interval` seconds to call `pred`.
  Keeps this up until either `pred` returns true or `:timeout` has elapsed.
  When `:timeout` has elapsed a slingshot exception is throws with `:message`.

  Arguments:

  - `pred`: a zero-argument predicate to call
  - `opts`: a map of optional parameters:
    - `:timeout` wait limit in seconds, [[*wait-timeout*]] by default;
    - `:interval` how long to wait between calls, [[*wait-interval*]] by default;
    - `:message` a message that becomes a part of exception when timeout is reached."

  ([pred]
   (wait-predicate pred {}))
  ([pred opts]
   (let [timeout   (get opts :timeout *wait-timeout*) ;; refactor this (call for java millisec)
         time-rest (get opts :time-rest timeout)
         interval  (get opts :interval *wait-interval*)
         times     (get opts :times 0)
         message   (get opts :message)]
     (when (< time-rest 0)
       (throw+ {:type      :etaoin/timeout
                :message   message
                :timeout   timeout
                :interval  interval
                :times     times
                :predicate pred}))
     (when-not (with-http-error
                 (pred))
       (wait interval)
       (recur pred (assoc
                     opts
                     :time-rest (- time-rest interval)
                     :times (inc times)))))))

(defn wait-exists
  "Waits until `driver` finds element [[exists?]] via `q`.

  - `opts`: see [[wait-predicate]] opts."

  [driver q & [opts]]
  (let [message (format "Wait until %s element exists" q)]
    (wait-predicate #(exists? driver q)
                    (merge {:message message} opts))))

(defn wait-absent
  "Waits until `driver` determines element is not found by `q` (is [[absent?]]).

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."

  [driver q & [opts]]
  (let [message (format "Wait until %s element is absent" q)]
    (wait-predicate #(absent? driver q)
                    (merge {:message message} opts))))

(defn wait-visible
  "Waits until `driver` determines element found by `q` is [[visible?]].

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."
  [driver q & [opts]]
  (let [message (format "Wait until %s element is visible" q)]
    (wait-predicate #(visible? driver q)
                    (merge {:message message} opts))))

(defn wait-invisible
  "Waits until `driver` determines element found by `q` is [[invisible?]].

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."
  [driver q & [opts]]
  (let [message (format "Wait until %s element is invisible" q)]
    (wait-predicate #(invisible? driver q)
                    (merge {:message message} opts))))

(defn wait-enabled
  "Waits until `driver` determines element found by `q` is [[enabled?]].

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."
  [driver q & [opts]]
  (let [message (format "Wait until %s element is enabled" q)]
    (wait-predicate #(enabled? driver q)
                    (merge {:message message} opts))))

(defn wait-disabled
  "Waits until `driver` determines element found by `q` is [[disabled?]].

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."
  [driver q & [opts]]
  (let [message (format "Wait until %s element is disabled" q)]
    (wait-predicate #(disabled? driver q)
                    (merge {:message message} opts))))

(defn wait-has-alert
  "Waits until `driver` finds page [[has-alert?]].

  - `opts`: see [[wait-predicate]] opts."
  [driver & [opts]]
  (let [message "Wait until element has alert"]
    (wait-predicate #(has-alert? driver)
                    (merge {:message message} opts))))

(defn wait-has-text
  "Waits until `driver` finds element via `q` with `text` anywhere inside it (including inner HTML).

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."
  [driver q text & [opts]]
  (let [message (format "Wait until %s element has text %s"
                        q text)]
    (wait-predicate #(has-text? driver q text)
                    (merge {:message message} opts))))

(defn wait-has-text-everywhere
  "Waits until `driver` finds `text` anywhere on the current page.

  - `opts`: see [[wait-predicate]] opts."
  [driver text & [opts]]
  (let [q {:xpath "*"}]
    (wait-has-text driver q text opts)))

(defn wait-has-class
  "Waits until `driver` finds element via `q` [[has-class?]] `class`.

  See [[query]] for details on `q`.

  - `opts`: see [[wait-predicate]] opts."
  [driver q class & [opts]]
  (let [message (format "Wait until %s element has class %s"
                        q class)]
    (wait-predicate #(has-class? driver q class)
                    (merge {:message message} opts))))

(defn wait-running
  "Waits until `driver` is reachable via its host and port via [[running?]].

  Throws
  - if using `:webdriver-url`.
  - if driver process is found to be no longer running while waiting

  - `opts`: see [[wait-predicate]] opts."
  [driver & [opts]]
  (when (:webdriver-url driver)
    (throw (ex-info "Not supported for driver using :webdriver-url" {})))
  (log/debugf "Waiting until %s:%s is running"
              (:host driver) (:port driver))
  (wait-predicate #(running? driver) (merge {:message "Wait until driver is running"} opts)))

;;
;; visible actions
;;

(defn click-visible
  "Waits until `driver` finds visible element via query `q` then clicks on it.

  - `opts`: see [[wait-predicate]] opts."
  [driver q & [opts]]
  (doto driver
    (wait-visible q opts)
    (click q)))

;;
;; touch
;;

(defmulti touch-tap
  "Have `driver` touch tap element found by query `q`.

  See [[query]] for details on `q`."
  {:arglists '([driver q])}
  dispatch-driver)

(defmethod touch-tap
  :chrome
  [driver q]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :touch :click]
            :data   {:element (query driver q)}}))

(defmulti touch-down
  "Have `driver` touch down
  - on element found by query `q`
  - or `x` `y` coordinate

  See [[query]] for details on `q`."
  {:arglists '([driver q] [driver x y])}
  dispatch-driver)

(defmethod touch-down
  :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-down driver x y)))
  ([driver x y]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :touch :down]
             :data   {:x (int x) :y (int y)}})))

(defmulti touch-up
  "Have `driver` touch up
  - on element found by query `q`
  - or `x` `y` coordinate

  See [[query]] for details on `q`."
  {:arglists '([driver q] [driver x y])}
  dispatch-driver)

(defmethod touch-up
  :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-down driver x y)))
  ([driver x y]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :touch :up]
             :data   {:x (int x) :y (int y)}})))

(defmulti touch-move
  "Have `driver` touch move
  - on element found by query `q`
  - or `x` `y` coordinate

  See [[query]] for details on `q`."
  {:arglists '([driver q] [driver x y])}
  dispatch-driver)

(defmethod touch-move
  :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-move driver x y)))
  ([driver x y]
   (execute {:driver driver
             :method :post
             :path   [:session (:session driver) :touch :move]
             :data   {:x (int x) :y (int y)}})))

;;
;; skip/when driver
;;

(defmacro when-not-predicate
  "Executes `body` when `predicate` returns falsy."
  [predicate & body]
  `(when-not (~predicate)
     ~@body))

(defmacro when-not-drivers
  "Executes `body` when browser `driver` is NOT in `browsers`, ex: `#{:chrome :safari}`"
  [browsers driver & body]
  `(when-not-predicate #((set ~browsers) (dispatch-driver ~driver)) ~@body))

(defmacro when-not-chrome
  "Executes `body` when browser `driver` is NOT Chrome."
  [driver & body]
  `(when-not-predicate #(chrome? ~driver) ~@body))

(defmacro when-not-edge
  "Executes `body` when browser `driver` is NOT Edge."
  [driver & body]
  `(when-not-predicate #(edge? ~driver) ~@body))

(defmacro when-not-phantom
  "Executes `body` when browser `driver` is NOT Phantom.js."
  [driver & body]
  `(when-not-predicate #(phantom? ~driver) ~@body))

(defmacro when-not-firefox
  "Executes `body` when browser `driver` is NOT Firefox."
  [driver & body]
  `(when-not-predicate #(firefox? ~driver) ~@body))

(defmacro when-not-safari
  "Executes `body` when browser `driver` is NOT Safari."
  [driver & body]
  `(when-not-predicate #(safari? ~driver) ~@body))

(defmacro when-not-headless
  "Executes `body` when browser `driver` is NOT in headless mode."
  [driver & body]
  `(when-not-predicate #(headless? ~driver) ~@body))

(defmacro when-predicate
  "Executes `body` when `predicate` returns truthy."
  [predicate & body]
  `(when (~predicate)
     ~@body))

(defmacro when-chrome
  "Executes `body` when browser `driver` is Chrome."
  [driver & body]
  `(when-predicate #(chrome? ~driver) ~@body))

(defmacro when-phantom
  "Executes `body` when browser `driver` is Phantom.js."
  [driver & body]
  `(when-predicate #(phantom? ~driver) ~@body))

(defmacro when-firefox
  "Executes `body` when browser `driver` is Firefox."
  [driver & body]
  `(when-predicate #(firefox? ~driver) ~@body))

(defmacro when-edge
  "Executes `body` when browser `driver` is Edge."
  [driver & body]
  `(when-predicate #(edge? ~driver) ~@body))

(defmacro when-safari
  "Executes `body` when browser `driver` is Safari."
  [driver & body]
  `(when-predicate #(safari? ~driver) ~@body))

(defmacro when-headless
  "Executes `body` when the `driver` is in headless mode."
  [driver & body]
  `(when-predicate #(headless? ~driver) ~@body))

;;
;; input
;;

(defn- codepoints
  "Clojure returns a seq of chars for a string.
  This does not handle wide (unicode) characters.
  Here we return a seq of codepoint strings for string `s`."
  [^String s]
  (->> s
       .codePoints
       .iterator
       iterator-seq
       (map #(String. (Character/toChars %)))))

(defn- make-input* [text & more]
  (codepoints (apply str text more)))

(defmulti fill-el
  "Have `driver` fill input element `el` with `text` (and optionally `more` text)."
  {:arglists '([driver el text & more])}
  dispatch-driver)

(defmethod fill-el
  :default
  [driver el text & more]
  {:pre [(some? el)]}
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :element el :value]
            :data   {:value (apply make-input* text more)}}))

(defmethods fill-el
  [:firefox :safari]
  [driver el text & more]
  {:pre [(some? el)]}
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :element el :value]
            :data   {:text (str/join (apply make-input* text more))}}))

(defmulti ^:private fill-active*
  {:arglists '([driver text & more])}
  dispatch-driver)

(defmethod fill-active*
  :chrome
  [driver text & more]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :keys]
            :data   {:value (apply make-input* text more)}}))

(defmethods fill-active*
  [:firefox :safari]
  [driver text & more]
  (let [el (get-active-element* driver)]
    (apply fill-el driver el text more)))

(defn fill-active
  "Have `driver` fill active element with `text` (and optionally `more` text)."
  [driver text & more]
  (apply fill-active* driver text more))

(defn fill
  "Have `driver` fill input element found by `q` with `text` (and optionally `more` text).

  See [[query]] for details on `q`.

  Example:
  ```Clojure
  (fill driver :simple-input \"foo\" \"baz\" 1)
  ;; fills the input with text: foobaz1
  ```"
  [driver q text & more]
  (apply fill-el driver (query driver q) text more))

(defn fill-multi
  "Have `driver` fill multiple inputs via `q-text`.

  `q-text` can be:
  - a map of `{q1 \"text1\" q2 \"text2\" ...}`
  - a vector of `[q1 \"text1\" q2 \"text2\" ...]`

  See [[query]] for details on `q`s."
  [driver q-text]
  (cond
    (map? q-text)
    (doseq [[q text] q-text]
      (fill driver q text))

    (vector? q-text)
    (recur driver (apply hash-map q-text))

    :else (throw+ {:type    :etaoin/argument
                   :message "Wrong argument type"
                   :arg     q-text})))

(defn fill-human-el
  "Have `driver` fill element `el` with `text` as if it were a real human using `opts`.

  `opts`
  - `:mistake-prob` probability of making a typo (0 to 1.0) (default: `0.1`)
  - `:pause-max` maximum amount of time in seconds to pause between keystrokes (can be fractional) (default: `0.2`)"
  [driver el text opts]
  {:pre [(some? el)]}
  (let [{:keys [mistake-prob pause-max]
         :or   {mistake-prob 0.1
                pause-max    0.2}} opts

        rand-char (fn [] (-> 26 rand-int (+ 97) char))
        wait-key  (fn [] (wait (min (rand) pause-max)))]
    (click-el driver el)
    (wait-key)
    (doseq [c (codepoints text)]
      (when (< (rand) mistake-prob)
        (fill-el driver el (rand-char))
        (wait-key)
        (fill-el driver el k/backspace)
        (wait-key))
      (fill-el driver el c)
      (wait-key))))

(defn fill-human
  "Have `driver` fill element found by `q` with `text` as if it were a real human using `opts`.

  See [[query]] for details on `q`.

  `opts`
  - `:mistake-prob` probability of making a typo (0 to 1.0) (default: `0.1`)
  - `:pause-max` maximum amount of time in seconds to pause between keystrokes (can be fractional) (default: `0.2`)"
  ([driver q text]  (fill-human driver q text {}))
  ([driver q text opts]
   (fill-human-el driver (query driver q) text opts)))

(defn fill-human-multi
  "Have `driver` fill multiple elements as if it were a real human being via `q-text` using `opts`.

  `q-text` can be:
  - a map of `{q1 \"text1\" q2 \"text2\" ...}`
  - a vector of `[q1 \"text1\" q2 \"text2\" ...]`

  See [[query]] for details on `q`s.

  `opts`
  - `:mistake-prob` probability of making a typo (0 to 1.0) (default: `0.1`)
  - `:pause-max` maximum amount of time in seconds to pause between keystrokes (can be fractional) (default: `0.2`)"
  ([driver q-text]  (fill-human-multi driver q-text {}))
  ([driver q-text opts]
   (cond
     (map? q-text)
     (doseq [[q text] q-text]
       (fill-human driver q text opts))

     (vector? q-text)
     (recur driver (apply hash-map q-text) opts)

     :else (throw+ {:type    :etaoin/argument
                    :message "Wrong argument type"
                    :arg     q-text}))))

(defn select
  "Convenience function to have `driver` select first option that includes `text` for select element found by query `q`.

  To appease a quirk of the Safari WebDriver, we click on the select element first, then the option.
  Other WebDriver implementations do not seem to need, but are not negatively impacted by, the click on the select element.

  See [[query]] for details on `q`.

  See [User Guide](/doc/01-user-guide.adoc#select-dropdown) for other ways to select options from dropdowns."
  [driver q text]
  (let [select-el (query driver q)]
    (click-el driver select-el)
    (let [option-el (query driver q {:tag :option :fn/has-text text})]
      (click-el driver option-el))))

(defn clear-el
  "Have `driver` clear input element `el`"
  [driver el]
  {:pre [(some? el)]}
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :element el :clear]}))

(defn clear
  "Have `driver` clear input element found by `q` (and optionally `more-qs`).

  See [[query]] for details on `q`."
  [driver q & more-qs]
  (doseq [q (cons q more-qs)]
    (clear-el driver (query driver q))))

;;
;; file upload
;;

(defrecord RemoteFile [file])
(defn remote-file
  "Wraps `remote-file-path` for use with [[upload-file]] to avoid local file existence check.

  Example usage:
  ```Clojure
  (upload-file (remote-file \"C:/Users/hello/url.txt\"))
  ```"
  [remote-file-path]
  (RemoteFile. remote-file-path))

(defmulti upload-file
  "Have `driver` attach a file `path` to a file input field element found by query `q`.

  Arguments:

  - `q` see [[query]] for details;
  - `file`
    - when a string or java.io.File object, the file must exist locally.
    - when [[remote-file]] file is assumed to exist remotely and no local existence check is performed.

  Under the hood, we send the file's name as a sequence of keys to the input."
  {:arglists '([driver q path])}
  (fn [_driver _q file]
    (type file)))

(defmethod upload-file String
  [driver q path]
  (upload-file driver q (fs/file path)))

(defmethod upload-file RemoteFile
  [driver q path]
  ;; directly send the path without any local file existence validation
  (fill driver q (:file path)))

(defmethod upload-file java.io.File
  [driver q ^java.io.File file]
  (let [path    (.getAbsolutePath file)
        message (format "File %s does not exist" path)]
    (if (.exists file)
      (fill driver q path)
      (throw+ {:type    :etaoin/file
               :message message
               :driver  driver}))))

;;
;; submit
;;

(defn submit
  "Have `driver` submit a form by sending the enter key to input element found by query `q`.

  See [[query]] for details on `q`."
  [driver q]
  (fill driver q k/enter))

;;
;; timeouts
;; https://github.com/SeleniumHQ/selenium/blob/bc19742bb0256c0cb73a47eec5361aa7a5743723/py/selenium/webdriver/remote/webdriver.py#L674
;; https://searchfox.org/mozilla-central/source/testing/webdriver/src/command.rs#529

(defmulti ^:private set-timeout*
  "Basic method to set a specific timeout."
  {:arglists '([driver type sec])}
  dispatch-driver)

(defmethod set-timeout*
  :default
  [driver type sec]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :timeouts]
            :data   {type (util/sec->ms sec)}}))

(defmethod set-timeout*
  :chrome
  [driver type sec]
  (execute {:driver driver
            :method :post
            :path   [:session (:session driver) :timeouts]
            :data   {:type type :ms (util/sec->ms sec)}}))

(defmulti set-script-timeout
  "Sets `driver` timeout `seconds` for executing JavaScript."
  {:arglists '([driver seconds])}
  dispatch-driver)

(defmethod set-script-timeout
  :default
  [driver seconds]
  (set-timeout* driver :script seconds))

(defmulti set-page-load-timeout
  "Sets `driver` timeout `seconds` for loading a page."
  {:arglists '([driver seconds])}
  dispatch-driver)

(defmethod set-page-load-timeout
  :default
  [driver sec]
  (set-timeout* driver :pageLoad sec))

(defmethod set-page-load-timeout
  :chrome
  [driver seconds]
  (set-timeout* driver "page load" seconds))

(defmulti set-implicit-timeout
  "Sets `driver` timeout `seconds` to find elements on the page."
  {:arglists '([driver seconds])}
  dispatch-driver)

(defmethod set-implicit-timeout
  :default
  [driver seconds]
  (set-timeout* driver :implicit seconds))

(defmulti ^:private get-timeout*
  "Basic method to get a map of all the timeouts."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod get-timeout*
  :default
  [driver]
  (:value (execute {:driver driver
                    :method :get
                    :path   [:session (:session driver) :timeouts]})))

(defn get-script-timeout
  "Returns `driver` timeout in seconds for executing JavaScript."
  [driver]
  (-> driver get-timeout* :script util/ms->sec))

(defn get-page-load-timeout
  "Returns `driver` timeout in seconds for loading a page."
  [driver]
  (-> driver get-timeout* :pageLoad util/ms->sec))

(defn get-implicit-timeout
  "Returns `driver` timeout in seconds to find elements on the page."
  [driver]
  (-> driver get-timeout* :implicit util/ms->sec))

(defmacro with-script-timeout
  "Execute `body` temporarily setting `driver` to timeout `seconds` for executing JavaScript.
  Useful for asynchronous scripts."
  [driver seconds & body]
  `(let [prev# (get-script-timeout ~driver)]
     (set-script-timeout ~driver ~seconds)
     (try
       ~@body
       (finally
         (set-script-timeout ~driver prev#)))))

;;
;; screenshots
;;

(defn- create-dirs-for-file [f]
  (when (fs/parent f)
    (-> f fs/parent fs/create-dirs)))

(defn- b64-decode [s]
  (-> (Base64/getDecoder)
      (.decode ^String s)))

(defmulti ^:private b64-to-file
  "Dumps a Base64-encoded string into a file.
  A file might be either a path or a `java.io.File` instance."
  {:arglists '([b64str file] [b64str filepath])}
  util/dispatch-types)

(defmethod b64-to-file
  [String java.io.File]
  [b64str ^java.io.File file]
  (b64-to-file b64str (.getAbsolutePath file)))

(defmethod b64-to-file
  [String String]
  [^String b64str filepath]
  (with-open [out (io/output-stream filepath)]
    (.write out ^bytes (b64-decode b64str))))

(defmulti screenshot
  "Have `driver` save a PNG format screenshot of the current page to `file`.
  Throws if screenshot is empty.

  `file` can be either a string or `java.io.File`, any missing parent directories are automatically created."
  {:arglists '([driver file])}
  dispatch-driver)

(defmethod screenshot :default
  [driver file]
  (let [resp   (execute {:driver driver
                         :method :get
                         :path   [:session (:session driver) :screenshot]})
        b64str (-> resp :value not-empty)]
    (when (not b64str)
      (util/error "Empty screenshot"))
    (create-dirs-for-file file)
    (b64-to-file b64str file)))

(defmulti screenshot-element
  "Have `driver` save a PNG format screenshot of the element found by query `q` to `file`.

  See [[query]] for details on `q`.

  `file` can be either a string or `java.io.File`, any missing parent directories are automatically created."
  {:arglists '([driver q file])}
  dispatch-driver)

(defmethod screenshot-element
  :default
  [_driver _q _file]
  (util/error "This driver doesn't support screenshots on elements."))

(defmethods screenshot-element
  [:chrome :edge :firefox]
  [driver q file]
  (let [el     (query driver q)
        resp   (execute {:driver driver
                         :method :get
                         :path   [:session (:session driver) :element el :screenshot]})
        b64str (-> resp :value not-empty)]
    (when (not b64str)
      (util/error "Empty screenshot, query: %s" q))
    (create-dirs-for-file file)
    (b64-to-file b64str file)))

(defn ^:no-doc make-screenshot-file-path
  [driver-type dir]
  (->> (.getTime (java.util.Date.))
       (format "-%d.png")
       (str (name driver-type))
       (fs/file dir)
       str))

(defmacro with-screenshots
  "Have `driver` save a PNG imge screenshot to `dir` after each form in `body` is executed.

  Filenames will contain an epoch timestamp."
  [driver dir & body]
  (let [screenshot-form# `(screenshot ~driver (make-screenshot-file-path (:type ~driver) ~dir))
        new-body         (interleave body (repeat screenshot-form#))]
    `(do ~@new-body)))

;;
;; print current page to PDF
;;

(defmulti print-page
  "Have `driver` print current HTML page to `file` in PDF format.

  `file` can be either a string or `java.io.File`, any missing parent directories are automatically created.

  `opts` map is optional:
  - `:orientation` is `:landscape` or `:portrait` (default)
  - `:scale` a number, defaults to `1`, min of `0.1`, max of `2`
  - `:background` defaults to `false`
  - `:page` (default is North American Letter size 8.5x11 inches)
    - `:width` in cm, defaults to `21.59`
    - `:height` in cm, defaults to `27.94`
  - `:margin`
    - `:top` in cm, defaults to`1`
    - `:bottom` in cm, defaults to `1`
    - `:left` in cm, defaults to `1`
    - `:right` in cm, default to `1`
  - `:shrinkToFit` defaults to `true`
  - `:pageRanges` a vector, 1-based pages to include, example `[\"1-3\" \"6\"]` or `[]` for all (default)


  At the time of this writing current WebDriver support:
  - chrome, works in headless mode only, supports all opts
  - edge, works in headless only, supports all opts
  - firefox, might not support all opts
  - safari, feature is not implemented"
  {:arglists '([driver file] [driver file opts])}
  dispatch-driver)

(defmethod print-page
  :default
  [_driver _file & [_opts]]
  (util/error "This driver doesn't support printing pages to PDF."))

(defmethods print-page
  [:chrome :edge :firefox]
  [driver file & [opts]]
  (let [resp   (execute {:driver driver
                         :method :post
                         :path   [:session (:session driver) :print]
                         :data opts})
        b64str (-> resp :value not-empty)]
    (when (not b64str)
      (util/error "Empty page"))
    (create-dirs-for-file file)
    (b64-to-file b64str file)))

;;
;; postmortem
;;

(defn- get-pwd []
  (System/getProperty "user.dir"))

(defn- join-path
  "Joins two and more path components into a single file path OS-wisely."
  [p1 p2 & more]
  (.getPath ^java.io.File (apply fs/file p1 p2 more)))

(defn- format-date
  [date pattern]
  (.format (SimpleDateFormat. pattern) date))

(defn ^:no-doc postmortem-handler
  "Internal postmortem handler that creates files.
  See the `with-postmortem`'s docstring below for more info."
  [driver {:keys [dir dir-src dir-img dir-log date-format]}]
  (let [dir     (or dir (get-pwd))
        dir-img (or dir-img dir)
        dir-src (or dir-src dir)
        dir-log (or dir-log dir)

        file-tpl "%s-%s-%s-%s.%s"

        date-format (or date-format "yyyy-MM-dd-HH-mm-ss")
        params      [(-> driver :type name)
                     (-> driver :host)
                     (-> driver :port)
                     (format-date (Date.) date-format)]

        file-img (apply format file-tpl (conj params "png"))
        file-src (apply format file-tpl (conj params "html"))
        file-log (apply format file-tpl (conj params "json"))

        path-img (join-path dir-img file-img)
        path-src (join-path dir-src file-src)
        path-log (join-path dir-log file-log)]

    (io/make-parents path-img)
    (io/make-parents path-src)
    (io/make-parents path-log)

    (log/debugf "Writing screenshot: %s" path-img)
    (screenshot driver path-img)

    (log/debugf "Writing HTML source: %s" path-src)
    (spit path-src (get-source driver))

    (when (supports-logs? driver)
      (log/debugf "Writing console logs: %s" path-log)
      (dump-logs (get-logs driver) path-log))))

(defmacro with-postmortem
  "Executes `body` with postmortem handling.
  Good for forensics.

  If an exception occurs, saves:
  - screenshot `.png` to `:dir-img` else `:dir` else current working directory
  - page source `.html` to `:dir-scr` else `:dir`  else current working directory
  - console log `.json` to `:dir-log` else `:dir` else current working directory

  Dirs are automatically created if necessary.

  `:date-format` used in filename to keep them unique.
  See Java SDK `SimpleDateFormat` for available patterns.
  Defaults to `\"yyyy-MM-dd-HH-mm-ss\"`.

  Tip: don't bother using `with-postmortem` in test fixtures.
  The standard `clojure.test`framework has its own way of handling exceptions,
  so wrapping a fixture with `(with-postmortem...)` would be in vain."
  [driver opts & body]
  `(try
     ~@body
     (catch Exception e#
       (postmortem-handler ~driver ~opts)
       (throw e#))))

;;
;; driver management
;;

(defn- make-url
  "Returns a WebDriver url string for `host` and `port`."
  [host port]
  (format "http://%s:%s" host port))

(defn- -create-driver
  "Creates a new driver instance.

  Returns a map that represents driver's state. Some functions, for
  example creating or deleting a session may change its state.

  The function does not start a process or open a window. It just
  creates a map without side effects.

  Arguments:

  - `type` is a keyword determines what driver to use. The supported
  browsers are `:firefox`, `:chrome`, `:phantom` and `:safari`.

  - `opts` is a map with additional options for a driver. The supported
  options are:

  -- `:host` is a string with either IP or hostname. Use it if the
  server is run not locally but somethere in your network.

  -- `:port` is an integer value what HTTP port to use.

  -- `:webdriver-url` is a URL to a web-driver service. This URL is
  generally provided by web-driver service providers. When specified the
  `:host` and `:port` parameters are ignored.

  -- `:locator` is a string determs what algorithm to use by default
  when finding elements on a page."
  [type & [{:keys [port host webdriver-url locator]}]]
  (let [host    (or host "127.0.0.1")
        url     (make-url host port)
        driver  (util/assoc-some {:type   type
                                  :host    host
                                  :port    port
                                  :url     url ;; NOTE: not great that this is also used on input to indicate default browser url
                                  :locator locator}
                                 :webdriver-url webdriver-url)]
    (if webdriver-url
      (log/debugf "Created driver: %s %s" (name type) (util/strip-url-creds webdriver-url))
      (log/debugf "Created driver: %s %s:%s" (name type) host port))
    driver))

(defn- proxy-env
  [proxy]
  (let [http (System/getenv "HTTP_PROXY")
        ssl  (System/getenv "HTTPS_PROXY")]
    (cond-> proxy
      http (assoc :http http)
      ssl  (assoc :ssl ssl))))

(defn- -run-driver*
  [driver & [{:keys [dev
                     env
                     log-level
                     log-stdout
                     log-stderr
                     args-driver
                     path-driver
                     download-dir
                     path-browser
                     driver-log-level]}]]
  (let [{:keys [port host]} driver

        _ (when (util/connectable? host port)
            (throw (ex-info
                     (format "Port %d already in use" port)
                     {:port port})))

        driver    (cond-> driver
                    true             (drv/set-browser-log-level log-level)
                    true             (drv/set-path path-driver)
                    true             (drv/set-port port)
                    dev              (drv/set-perf-logging (:perf dev))
                    driver-log-level (drv/set-driver-log-level driver-log-level)
                    args-driver      (drv/set-args args-driver)
                    path-browser     (drv/set-binary path-browser)
                    download-dir     (drv/set-download-dir download-dir))
        proc-args (drv/get-args driver)
        _         (log/debugf "Starting process: %s" (str/join \space proc-args))
        process   (proc/run proc-args {:log-stdout log-stdout
                                       :log-stderr log-stderr
                                       :env        env})]
    (util/assoc-some driver :env env :process process)))

(defn- -run-driver
  "Runs a driver process locally.

  Creates a UNIX process with a Webdriver HTTP server. Host and port
  are taken from a `driver` argument. Updates a driver instance with
  new fields with process information. Returns modified driver.

  Arguments:

  - `driver` is a map created with `-create-driver` function.

  - `opts` is an optional map with the following possible parameters:

  -- `:path-driver` is a string path to the driver's binary file.

  -- `:path-browser` is a string path to the browser's binary
  file. When not passed, the driver discovers it by its own.

  -- `:webdriver-failed-launch-retries` number of times to retry launching webdriver process.

  -- `:log-level` a keyword to set browser's log level. Used when fetching
  browser's logs. Possible values are: `:off`, `:debug`, `:warn`, `:info`,
  `:error`, `:all`.

  -- `:driver-log-level` a keyword to set driver's log level.
  The value is a string. Possible values are:
  chrome: [ALL, DEBUG, INFO, WARNING, SEVERE, OFF]
  phantomjs: [ERROR, WARN, INFO, DEBUG] (default INFO)
  firefox [fatal, error, warn, info, config, debug, trace]

  -- `:log-stdout` and `:log-stderr`. Paths to the driver's log files as strings.
  Specify `:inherit` to inherit destination from calling process (ex. console).
  When not set, the output goes to /dev/null (or NUL on Windows)

  -- `:args-driver` is a vector of additional arguments to the
  driver's process.

  -- `:env` is a map with system ENV variables. Keys are turned into
  upper-case strings."
  [driver {:keys [webdriver-failed-launch-retries] :as opts}]
  (let [max-tries (inc webdriver-failed-launch-retries)]
    (loop [try-num 1
           ex nil]
      (if (> try-num max-tries)
        (throw (ex-info (format "gave up trying to launch %s after %d tries" (:type driver) max-tries) {} ex))
        (do
          (when ex
            (log/warnf ex "unexpected exception occurred launching %s, try %d (of a max of %d)"
                       (:type driver) (dec try-num) max-tries)
            (Thread/sleep 100))
          (let [driver (-run-driver* driver opts)
                res (try
                      (wait-running driver)
                      (catch Exception e
                        {:exception e}))]
            (if (not (:exception res))
              driver
              (recur (inc try-num) (:exception res)))))))))


(defn- -connect-driver
  "Connects to a running Webdriver server.

  Creates a new session on Webdriver HTTP server. Sets the session to
  the driver. Returns the modified driver.

  Arguments:

  - `opts`: an map of the following optional parameters:

  -- `:capabilities` a map of desired capabilities your
  browser should support;

  -- `:desired-capabilities`: an alias for `:capabilities`.

  -- `headless` is a boolean flag to run the browser in headless mode
  (i.e. without GUI window). Useful when running tests on CI servers
  rather than local machine. Currently, only FF and Chrome support headless mode.
  Phantom.js is headless by its nature.

  -- `:size` is a vector of two integers specifying initial window size.

  -- `:url` is a string with the default URL opened by default (FF only for now).

  -- `:load-strategy` is a string or keyword with specifying
  what strategy to use when load a page. Might be `:none`, `:eager`
  or :`normal` (default). To not wait the page being loaded completely,
  specify `:none`. The `:eager` option is still under development
  in most of the browser.

  -- `:prefs` is a map of browser-specific preferences.

  -- `:profile` is a string path that points on profile folder.
  See the `Setting browser profile` section in `README.md` to know
  how to do it properly.

  -- `proxy` is a map of proxy server connection settings.

  --- `http` is a string. Defines the proxy host for HTTP traffic.
  --- `ssl` is a string. Defines the proxy host for encrypted TLS traffic.
  --- `ftp` is a string. Defines the proxy host for FTP traffic.
  --- `pac-url` is a string. Defines the URL for a proxy auto-config file.
  --- `bypass` is a vector. Lists the address for which the proxy should be bypassed.
  --- `socks` is a map.
  ---- `host` is a string. Defines the proxy host for a SOCKS proxy.
  ---- `version` Any integer between 0 and 255 inclusive. Defines the SOCKS proxy version.

  -- `:args` is a vector of additional command line arguments
  to the browser's process.

  See https://www.w3.org/TR/webdriver/#capabilities"
  [driver & [{:keys [webdriver-url
                     url ;; NOTE: somewhat confusing because we also set a webdriver :url in returned driver
                     size
                     args
                     prefs
                     proxy
                     profile
                     headless
                     user-agent
                     capabilities
                     load-strategy
                     desired-capabilities]}]]
  (when (not webdriver-url)
    (wait-running driver))
  (let [type          (:type driver)
        proxy         (proxy-env proxy)
        [with height] size
        driver        (cond-> driver
                        size          (drv/set-window-size with height)
                        url           (drv/set-url url)
                        headless      (drv/set-headless)
                        args          (drv/set-options-args args)
                        proxy         (drv/set-proxy proxy)
                        load-strategy (drv/set-load-strategy load-strategy)
                        prefs         (drv/set-prefs prefs)
                        profile       (drv/set-profile profile)
                        user-agent    (drv/set-user-agent user-agent)
                        :always
                        ;; NOTE: defaults overriding specific capabilities potentially set by above seems suspect
                        ;; but... maybe... not worth worrying about?
                        (->
                         (drv/set-capabilities (:capabilities defaults-global))
                         (drv/set-capabilities (get-in defaults [type :capabilities]))
                         (drv/set-capabilities capabilities)
                         (drv/set-capabilities desired-capabilities)))
        caps          (:capabilities driver)
        session       (create-session driver caps)]
    (assoc driver :session session)))

(defn disconnect-driver
  "Returns new `driver` after disconnecting from a running WebDriver process.

  Closes the current session that is stored in the driver if it still exists.
  Removes the session from `driver`."
  [driver]
  (try (delete-session driver)
       (catch Exception e
         (when (not (= 404 (:status (ex-data e))))
           ;; the exception was caused by something other than "session not found"
           (throw e))))

  (dissoc driver :session))

(defn stop-driver
  "Returns new `driver` after killing its WebDriver process."
  [driver]
  (proc/kill (:process driver))
  (dissoc driver :process :args :env :capabilities))

(defn quit
  "Have `driver` close the current session, then, if Etaoin launched it, kill the WebDriver process."
  [driver]
  (let [process (:process driver)]
    (try
      (disconnect-driver driver)
      (finally
        (when process
          (stop-driver driver))))))

(defn boot-driver
  "Launch and return a driver of `type` (e.g. `:chrome`, `:firefox` `:safari` `:edge` `:phantom`)
  with `opts` options.

  - creates a driver
  - launches a WebDriver process (or connects to an existing running process if `:host`
  or `:webdriver-url` is specified)
  - creates a session for driver

  Defaults taken from [[defaults-global]] then [[defaults]] for `type`:
  `:port` - if `:host` not specified, port is randomly generated for local WebDriver process
  `:capabilities` - are deep merged as part of connect logic.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  ([type]
   (boot-driver type {}))
  ([type {:keys [host webdriver-url] :as opts}]
   (let [default-opts (cond-> (merge (dissoc defaults-global :capabilities)
                                     (dissoc (type defaults) :capabilities))
                        ;; if host, we are launching webdriver, default port is random
                        (not host) (assoc :port (util/get-free-port)))
         opts (merge default-opts opts)
         driver (cond-> (-create-driver type opts)
                  (and (not host) (not webdriver-url)) (-run-driver opts))]
     (try
       (-connect-driver driver opts)
       (catch Throwable ex
         (when (:process driver)
           (try
             (quit driver)
             ;; silently ignore failure to quit driver on cleanup
             (catch Throwable _ex)))
         (throw ex))))))

(def ^{:arglists '([] [opts])} firefox
  "Launch and return a Firefox driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  (partial boot-driver :firefox))

(def ^{:arglists '([] [opts])} edge
  "Launch and return an Edge driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  (partial boot-driver :edge))

(def ^{:arglists '([] [opts])} chrome
  "Launch and return a Chrome driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  (partial boot-driver :chrome))

(def ^{:arglists '([] [opts])} phantom
  "Launch and return a Phantom.js driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  (partial boot-driver :phantom))

(def ^{:arglists '([] [opts])} safari
  "Launch and return a Safari driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  (partial boot-driver :safari))

(defn chrome-headless
  "Launch and return a headless Chrome driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  ([]
   (chrome-headless {}))
  ([opt]
   (boot-driver :chrome (assoc opt :headless true))))

(defn firefox-headless
  "Launch and return a headless Firefox driver.

  `opts` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  ([]
   (firefox-headless {}))
  ([opts]
   (boot-driver :firefox (assoc opts :headless true))))

(defn edge-headless
  "Launch and return a headless Edge driver.

  `opt` map is optionally, see [Driver Options](/doc/01-user-guide.adoc#driver-options)."
  ([]
   (edge-headless {}))
  ([opts]
   (boot-driver :edge (assoc opts :headless true))))

(defmacro with-driver
  "Executes `body` with a driver session of `type` (e.g. `:chrome`, `:firefox` `:safari` `:edge` `:phantom`)
  with `opts` options, binding driver instance to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-driver :firefox driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([type opts? bind & body])}
  [type & args]
  (let [[opts bind & body] (if (symbol? (second args))
                             args
                             (cons nil args))]
    `(client/with-pool {}
       (let [~bind (boot-driver ~type ~opts)]
         (try
           ~@body
           (finally
             (quit ~bind)))))))

(defmacro ^:no-doc with-headless-driver
  {:arglists '([type opts? bind & body])}
  [type & args]
  (let [[opts bind & body] (if (symbol? (second args))
                             args
                             (cons nil args))]
    `(let [opts# (assoc ~opts :headless true)]
       (with-driver ~type opts# ~bind ~@body))))

(defmacro with-firefox
  "Executes `body` with a Firefox driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-firefox driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body] [bind & body])}
  [& args]
  `(with-driver :firefox ~@args))

(defmacro with-chrome
  "Executes `body` with a Chrome driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-chrome driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-driver :chrome ~@args))

(defmacro with-edge
  "Executes `body` with an Edge driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-edge driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-driver :edge ~@args))

(defmacro with-phantom
  "Executes `body` with an Phantom.JS driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-phantom driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-driver :phantom ~@args))

(defmacro with-safari
  "Executes `body` with a Safari driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-safari driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-driver :safari ~@args))

(defmacro with-chrome-headless
  "Executes `body` with a headless Chrome driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-chrome-headless driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-headless-driver :chrome ~@args))

(defmacro with-firefox-headless
  "Executes `body` with a headless Firefox driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-firefox-headless driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-headless-driver :firefox ~@args))

(defmacro with-edge-headless
  "Executes `body` with a headless Edge driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` map can be omitted, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:
  ```Clojure
  (with-edge-headless driver
    (go driver \"https://clojure.org\"))
  ```"
  {:arglists '([opts? bind & body])}
  [& args]
  `(with-headless-driver :edge ~@args))
