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
  https://github.com/mozilla/geckodriver
  https://github.com/mozilla/webdriver-rust/

  Phantom.js (Ghostdriver)
  https://github.com/detro/ghostdriver/blob/
  "
  (:require [etaoin.legacy] ;; patch legacy clojure.string first
            [etaoin.proc :as proc]
            [etaoin.client :as client]
            [etaoin.keys :as keys]
            [etaoin.query :as query]
            [etaoin.util :as util :refer [defmethods]]
            [etaoin.driver :as drv]
            [etaoin.xpath :as xpath]
            [clojure.data.codec.base64 :as b64]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :refer [generate-stream]]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import java.util.Date
           java.net.ConnectException
           java.text.SimpleDateFormat))

;;
;; defaults
;;

(def defaults
  {:firefox {:port 4444
             :path "geckodriver"}
   :chrome {:port 9515
            :path "chromedriver"}
   :phantom {:port 8910
             :path "phantomjs"}
   :safari {:port 4445
            :path "safaridriver"}})

(def default-locator "xpath")
(def locator-xpath "xpath")
(def locator-css "css selector")

;;
;; utils
;;

(defn dispatch-driver
  "Returns the current driver's type. Used as dispatcher in
  multimethods."
  [driver & _]
  (:type @driver))

(defn- implemented?
  [driver feature]
  (when (get-method feature (:type @driver))
    true))

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
  [driver & [capabilities]]
  (with-resp driver
    :post
    [:session]
    {:desiredCapabilities (or capabilities {})}
    result
    (or (:sessionId result)             ;; default
        (:sessionId (:value result))))) ;; firefox

(defn delete-session [driver]
  "Deletes a session. Closes a browser window."
  (with-resp driver
    :delete
    [:session (:session @driver)]
    nil _))

;;
;; active element
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

(defmethods get-active-element*
  [:chrome :phantom :safari]
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
  {:arglists '([driver])}
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
  {:arglists '([driver])}
  dispatch-driver)

(defmethod get-window-handles :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window :handles]
    nil resp
    (:value resp)))

(defmethods get-window-handles
  [:chrome :phantom]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :window_handles]
    nil resp
    (:value resp)))

(defmulti switch-window
  "Switches a browser to another window."
  {:arglists '([driver handle])}
  dispatch-driver)

(defmethod switch-window
  :default
  [driver handle]
  (with-resp driver :post
    [:session (:session @driver) :window]
    {:handle handle} _))

(defmethod switch-window
  :chrome
  [driver handle]
  (with-resp driver :post
    [:session (:session @driver) :window]
    {:name handle} _))

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
  {:arglists '([driver])} ;; todo it does't work
  dispatch-driver)

(defmethod maximize :firefox
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :window :maximize]
    nil _))

(defmethods maximize
  [:chrome :safari]
  [driver]
  (let [h (get-window-handle driver)]
    (with-resp driver :post
      [:session (:session @driver) :window h :maximize]
      nil _)))

(defmulti get-window-size
  "Returns a window size a map with `:width` and `:height` keys."
  {:arglists '([driver])}
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
  {:arglists '([driver])}
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

(def reload refresh) ;; just an alias

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
;; Finding element(s)
;;

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

(defmulti find-elements* dispatch-driver)

(defmethod find-elements* :default
  [driver locator term]
  (with-resp driver :post
    [:session (:session @driver) :elements]
    {:using locator :value term}
    resp
    (->> resp :value (mapv (comp second first)))))

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

(defmulti find-elements-from* dispatch-driver)

(defmethod find-elements-from* :default
  [driver el locator term]
  (with-resp driver :post
    [:session (:session @driver) :element el :elements]
    {:using locator :value term}
    resp
    (->> resp :value (mapv (comp second first)))))

;;
;; Querying elements (high-level API)
;;

(defn query
  "Finds an element on a page.

   A query might be:

   - a string with an XPath expression;
   - a keyword `:active` that means to get the current active element;
   - any other keyword which stands for an element's ID attribute;
   - a map with either `:xpath` or `:css` key with a string value
     of corresponding selector type (XPath or CSS);
   - any other map that will be expanded into XPath term (see README.md);
   - a vector of any expressions mentioned above. In that case, each next
     term is searched from the previous one.

   Returns a element's unique identifier."

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
  "Finds multiple elements on a page.
  See `query` function for incoming params.
  Returns a vector of element identifiers."

  ([driver q]
   (cond

     (vector? q)
     (apply query-all driver q)

     :else
     (let [[loc term] (query/expand driver q)]
       (find-elements* driver loc term))))

  ([driver q & more]
   (let [[loc term] (query/expand driver (last more))
         el (apply query driver q (butlast more))]
     (find-elements-from* driver el loc term))))

;;
;; mouse
;;

(defmulti mouse-btn-down
  "Puts down a button of a virtual mouse."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods mouse-btn-down
  [:chrome :phantom :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :buttondown]
    nil _))

(defmulti mouse-btn-up
  "Puts up a button of a virtual mouse."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods mouse-btn-up
  [:chrome :phantom :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :buttonup]
    nil _))

(defmulti mouse-move-to
  "Moves a virtual mouse pointer either to an element
  or by `x` and `y` offset."
  {:arglists '([driver q] [driver x y])}
  dispatch-driver)

(defmethods mouse-move-to
  [:chrome :phantom :safari]
  ([driver q]
   (with-resp driver :post
     [:session (:session @driver) :moveto]
     {:element (query driver q)} _))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :moveto]
     {:xoffset x :yoffset y} _)))

(defmacro with-mouse-btn
  "Performs the body keeping mouse botton pressed."
  [driver & body]
  `(do
     (mouse-btn-down ~driver)
     (try
       ~@body
       (finally
         (mouse-btn-up ~driver)))))

(defn drag-and-drop
  "Performs drag and drop operation as a sequence of the following steps:

  1. moves mouse pointer to an element found with `q-from` query;
  2. puts down mouse button;
  3. moves mouse to an element found with `q-to` query;
  4. puts up mouse button.

  Arguments:

  - `driver`: a driver instance,

  - `q-from`: from what element to start dragging; any expression that
  `query` function may accept;

  - `q-to`: to what element to drag, a seach term.

  Notes:

  - does not work in Phantom.js since it does not have a virtual mouse API;

  - does not work in Safari.
"
  [driver q-from q-to]
  (mouse-move-to driver q-from)
  (with-mouse-btn driver
    (mouse-move-to driver q-to)))

;;
;; click
;;

(defn click-el [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :click]
    nil _))

(defn click
  "Clicks on an element (a link, a button, etc)."
  [driver q]
  (click-el driver (query driver q)))

(defmulti double-click-el dispatch-driver)

(defmethods double-click-el
  [:chrome :phantom]
  [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :doubleclick]
    nil _))

(defn double-click
  "Performs double click on an element.

  Note:

  the supported browsers are Chrome, and Phantom.js.
  For Firefox and Safari, your may try to simulate it as a `click, wait, click`
  sequence."
  [driver q]
  (double-click-el driver (query driver q)))

;;
;; element size
;;

(defmulti get-element-size-el dispatch-driver)

(defmethods get-element-size-el
  [:chrome :phantom :safari]
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

(defn get-element-size
  "Returns an element size as a map with :width and :height keys."
  [driver q]
  (get-element-size-el driver (query driver q)))

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
  "Returns an element location on a page as a map with :x and :x keys."
  (get-element-location-el driver (query driver q)))

;;
;; element box
;;

(defn get-element-box
  "Returns a bounding box for an element found with a query term.

  The result is a map with the following keys:

  - `:x1`: top left `x` coordinate;
  - `:y1`: top left `y` coordinate;
  - `:x2`: bottom right `x` coordinate;
  - `:y2`: bottom right `y` coordinate;
  - `:width`: width as a difference b/w `:x2` and `:x1`;
  - `:height`: height as a difference b/w `:y2` and `:y1`.
"
  [driver q]
  (let [el (query driver q)
        {:keys [width height]} (get-element-size-el driver el)
        {:keys [x y]} (get-element-location-el driver el)]
    {:x1 x
     :x2 (+ x width)
     :y1 y
     :y2 (+ y height)
     :width width
     :height height}))

(defn intersects?
  "Determines whether two elements intersects in geometry meaning.

  The implementation compares bounding boxes for each element
  analyzing their arrangement.

  Arguments:

  - `q1` and `q2` are query terms to find elements to check for
  intersection.

  Returns true or false.
"
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

(defn- get-element-property-el
  [driver el property]
  (with-resp driver :get
    [:session (:session @driver) :element el :property (name property)]
    nil
    resp
    (:value resp)))

(defn get-element-property
  "Returns a property of an element (value, etc).

  Arguments:

  - `driver`: a driver instance,

  - `q`: a query term to find an element,

  - `name`: either a string or a keyword with a name of a property.

  Returns: a string with the attribute value, `nil` if no such
  property for that element."
  [driver q name]
  (get-element-property-el driver (query driver q) name))

(defn get-element-properties
  "Returns multiple properties in batch. The result is a vector of
  corresponding properties."
  [driver q & props]
  (let [el (query driver q)]
    (vec
     (for [prop props]
       (get-element-property-el driver el prop)))))

;;
;; attributes
;;

(defn get-element-attr-el
  [driver el attr]
  (with-resp driver :get
    [:session (:session @driver) :element el :attribute (name attr)]
    nil
    resp
    (:value resp)))

(defn get-element-attr
  "Returns an HTTP attribute of an element (class, id, href, etc).

  Arguments:

  - `driver`: a driver instance,

  - `q`: a query term to find an element,

  - `name`: either a string or a keyword with a name of an attribute.

  Returns: a string with the attribute value, `nil` if no such
  attribute for that element.

  Note: it does not split CSS classes! A single string with spaces is
  returned.

  Example:

  (def driver (firefox))
  (get-element-attr driver {:tag :a} :class)
  >> \"link link__external link__button\" ;; see note above
"
  [driver q name]
  (get-element-attr-el driver (query driver q) name))

(defn get-element-attrs
  "Returns multiple attributes in batch. The result is a vector of
  corresponding attributes."
  [driver q & attrs]
  (let [el (query driver q)]
    (vec
     (for [attr attrs]
       (get-element-attr-el driver el attr)))))

;;
;; css
;;

(defn- get-element-css-el
  [driver el name*]
  (with-resp driver :get
    [:session (:session @driver) :element el :css (name name*)]
    nil
    resp
    (-> resp :value not-empty)))

(defn get-element-css
  "Returns a CSS property of an element. The property might be both
  own or inherited.

  Arguments:

  - `driver`: a driver instance,

  - `q`: a query term,

  - `name`: a string/keyword with a CSS name (:font, \"background-color\", etc).

  Returns a string with a value, `nil` if there is no such property.

  Note: colors, fonts and some other properties may be represented in
  their own ways depending on a browser.

  Example:

  (def driver (firefox))
  (get-element-css driver {:id :content} :background-color)
  >> \"rgb(204, 204, 204)\" ;; or \"rgba(204, 204, 204, 1)\"
"
  [driver q prop]
  (get-element-css-el driver (query driver q) prop))


(defn get-element-csss
  "Returns multiple CSS properties in batch. The result is a vector of
  corresponding properties."
  [driver q & props]
  (let [el (query driver q)]
    (vec
     (for [prop props]
       (get-element-css-el driver el prop)))))

;;
;; element text, name and value
;;

(defn get-element-tag-el
  "Returns element's tag name by its identifier."
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :name]
    nil
    resp
    (:value resp)))

(defn get-element-tag
  "Returns element's tag name (\"div\", \"input\", etc)."
  [driver q]
  (get-element-tag-el driver (query driver q)))

(defn get-element-text-el
  "Returns element's inner text by its identifier."
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :text]
    nil
    resp
    (:value resp)))

(defn get-element-text
  "Returns inner element's text.

  For `<p class=\"foo\">hello</p>` it will be \"hello\" string.
"
  [driver q]
  (get-element-text-el driver (query driver q)))

;;
;; Element value
;;

(defn- get-element-value-el
  "Low level: returns element's value by its identifier."
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :value]
    nil
    resp
    (:value resp)))

(defmulti get-element-value
  "Returns the current element's value (input text)."
  {:arglists '([driver q])}
  dispatch-driver)

(defmethod get-element-value
  :default
  [driver q]
  (get-element-value-el driver (query driver q)))

(defmethods get-element-value
  [:safari :phantom]
  [driver q]
  (get-element-attr driver q :value))

(defmethod get-element-value
  :firefox
  [driver q]
  (get-element-property driver q :value))

;;
;; cookes
;;

(defn get-cookies
  "Returns all the cookies browser keeps at the moment.

  Each cookie is a map with structure:

  {:name \"cookie1\",
   :value \"test1\",
   :path \"/\",
   :domain \"\",
   :expiry nil,
   :secure false,
   :httpOnly false}
"
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :cookie]
    nil
    resp
    (:value resp)))

(defn get-cookie
  "Returns the first cookie with such name.

  Arguments:

  - `driver`: a driver instance,

  - `cookie-name`: a string/keyword witn a cookie name.
"
  [driver cookie-name]
  (->> driver
       get-cookies
       (filter #(= (:name %) (name cookie-name)))
       first))

(defn set-cookie
  "Sets a new cookie.

  Arguments:

  - `driver`: a driver instance,

  - `cookie`: a map with structure described in `get-cookies`. At
  least `:name` and `:value` fields should be populated.
"
  [driver cookie]
  (with-resp driver :post
    [:session (:session @driver) :cookie]
    {:cookie cookie}
    _))

(defn delete-cookie
  "Deletes a cookie by its name."
  [driver cookie-name]
  (with-resp driver :delete
    [:session (:session @driver) :cookie (name cookie-name)]
    nil _))

(defmulti delete-cookies
  "Deletes all the cookies for all domains."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod delete-cookies :default
  [driver]
  (with-resp driver :delete
    [:session (:session @driver) :cookie]
    nil _))

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
  "Returns browser's current HTML markup as a string."
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :source]
    nil
    resp
    (:value resp)))

;;
;; Javascript
;;

(defn el->ref
  "Turns machinery-wise element ID into an object
  that Javascript use to reference existing DOM element.

  The magic constant below is taken from the standard:
  https://www.w3.org/TR/webdriver/#elements

  Passing such an object to `js-execute` automatically expands into a
  DOM node. For example:

  ;; returns long UUID
  (def el (query driver :button-ok))

  ;; the first argument will the an Element instance.
  (js-execute driver \"arguments[0].scrollIntoView()\", (el->ref el))
  "
  [el]
  {:ELEMENT el
   :element-6066-11e4-a52e-4f735466cecf el})

(defmulti js-execute
  "Executes Javascript code in browser synchronously.

  The code is sent as a string (might be multi-line). Under the hood,
  a browser wraps your code into a function so avoid using `function`
  clause at the top level.

  Don't forget to add `return <something>` operator if you are
  interested in the result value.

  You may access arguments through the built-in `arguments`
  pseudo-array from your code. You may pass any data structures that
  are JSON-compatible (scalars, maps, vectors).

  The result value is also returned trough JSON encode/decode
  pipeline (JS objects turn to Clojure maps, arrays into vectors and
  so on).

  Arguments:

  - `driver`: a driver instance,

  - `script`: a string with the code to execute.

  - `args`: additional arguments for your code. Any data that might be
  serialized into JSON.

  Example:

  (def driver (chrome))
  (js-execute driver \"return arguments[0] + 1;\" 42)
  >> 43
  "
  {:arglists '([driver script & args])}
  dispatch-driver)

(defmethod js-execute :default
  [driver script & args]
  (with-resp driver :post
    [:session (:session @driver) :execute]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defmethod js-execute :firefox
  [driver script & args]
  (with-resp driver :post
    [:session (:session @driver) :execute :sync]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defmulti js-async
  "Executes an asynchronous script in the browser and returns the result.
  An asynchronous script is a such one that performs any kind of IO operations,
  say, AJAX request to the server. When running such kind of a script, you cannot
  just use the `return` statement like you do in ordinary scripts. Instead, the
  driver passes a special handler as the last argument that should be called
  to return the final result.

  *Note:* calling this function requires the `script` timeout to be set properly,
  meaning non-zero positive value. See `get-script-timeout`, `get-script-timeout`
  and `with-script-timeout` functions/macroses.

  Example of a script:

  // the `arguments` would be an array of something like:
  // [1, 2, true, ..., <special callback>]

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

  Arguments:

  - `driver`: a driver instance,

  - `script`: a string with the code to execute.

  - `args`: additional arguments for your code. Any data that might be
  serialized into JSON.
  "
  {:arglists '([driver script & args])}
  dispatch-driver)

(defmethod js-async :default
  [driver script & args]
  (with-resp driver :post
    [:session (:session @driver) :execute_async]
    {:script script :args (vec args)}
    resp
    (:value resp)))

(defmethod js-async :firefox
  [driver script & args]
  (with-resp driver :post
    [:session (:session @driver) :execute :async]
    {:script script :args (vec args)}
    resp
    (:value resp)))

;;
;; Javascript helpers
;;

(defn js-localstorage-clear
  [driver]
  (js-execute driver "localStorage.clear()"))

(defn add-script [driver url]
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
  "Scrolls the window into absolute position (jumps to exact place)."
  ([driver x y]
   (js-execute driver "window.scroll(arguments[0], arguments[1]);" x y))
  ([driver {:keys [x y]}]
   (scroll driver x y)))

(defn scroll-by
  "Scrolls the window by offset (relatively the current position)."
  ([driver x y]
   (js-execute driver "window.scrollBy(arguments[0], arguments[1]);" x y))
  ([driver {:keys [x y]}]
   (scroll-by driver x y)))

(defn scroll-query
  "Scrolls to the first element found with a query.

  Invokes element's `.scrollIntoView()` method. Accepts extra `param`
  argument that might be either boolean or object for more control.

  See this page for details:
  https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView
  "
  ([driver q]
   (let [el (query driver q)]
     (js-execute driver "arguments[0].scrollIntoView();" (el->ref el))))
  ([driver q param]
   (let [el (query driver q)]
     (js-execute driver "arguments[0].scrollIntoView(arguments[1]);" (el->ref el) param))))

(defn get-scroll
  "Returns the current scroll position as a map
  with `:x` and `:y` keys and integer values."
  [driver]
  (js-execute driver "return {x: window.scrollX, y: window.scrollY};"))

(defn scroll-top
  "Scrolls to top of the page keeping current horizontal position."
  [driver]
  (let [{:keys [x y]} (get-scroll driver)]
    (scroll driver x 0)))

(defn scroll-bottom
  "Scrolls to bottom of the page keeping current horizontal position."
  [driver]
  (let [y-max (js-execute driver "return document.body.scrollHeight;")
        {:keys [x y]} (get-scroll driver)]
    (scroll driver x y-max)))

(def ^{:doc "Default scroll offset in pixels."}
  scroll-offset 100)

(defn scroll-up
  "Scrolls the page up by specific number of pixels.
  The `scroll-offset` constant is used when not passed."
  ([driver offset]
   (scroll-by driver 0 (- offset)))
  ([driver]
   (scroll-up driver scroll-offset)))

(defn scroll-down
  "Scrolls the page down by specific number of pixels.
  The `scroll-offset` constant is used when not passed."
  ([driver offset]
   (scroll-by driver 0 offset))
  ([driver]
   (scroll-down driver scroll-offset)))

(defn scroll-left
  "Scrolls the page left by specific number of pixels.
  The `scroll-offset` constant is used when not passed."
  ([driver offset]
   (scroll-by driver (- offset) 0))
  ([driver]
   (scroll-left driver scroll-offset)))

(defn scroll-right
  "Scrolls the page right by specific number of pixels.
  The `scroll-offset` constant is used when not passed."
  ([driver offset]
   (scroll-by driver offset 0))
  ([driver]
   (scroll-right driver scroll-offset)))

;;
;; iframes
;;

(defn switch-frame*
  "Switches to an (i)frame by its index or an element reference."
  [driver id]
  (with-resp driver :post
    [:session (:session @driver) :frame]
    {:id id}
    _))

(defn switch-frame
  "Switches to an (i)frame quering the page for it."
  [driver q]
  (let [el (query driver q)]
    (switch-frame* driver (el->ref el))))

(defn switch-frame-first
  "Switches to the first (i)frame."
  [driver]
  (switch-frame* driver 0))

(defn switch-frame-parent
  "Switches to the parent of the current (i)frame."
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :frame :parent]
    nil
    _))

(defn switch-frame-top
  "Switches to the most top of the page."
  [driver]
  (switch-frame* driver nil))

(defmacro with-frame
  "Switches to the (i)frame temporary while executing the body
  returning the result of the last expression."
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
  "Returns a set of log types the browser supports."
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-log-types
  [:chrome :phantom]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :log :types]
    nil
    result
    (:value result)))

(defn- process-log
  "Remaps some of the log's fields."
  [entry]
  (-> entry
      (update :level (comp keyword str/lower-case))
      (update :source keyword)
      (assoc :datetime (-> entry :timestamp java.util.Date.))))

(defmulti get-logs
  "Returns Javascript log entries. Each log entry is a map
  with the following structure:

  {:level :warning,
   :message \"1,2,3,4  anonymous (:1)\",
   :timestamp 1511449388366,
   :source nil,
   :datetime #inst \"2017-11-23T15:03:08.366-00:00\"}

  Empirical knowledge about browser differences:

  * Chrome:
  - Returns all recorded logs.
  - Clears the logs once they have been read.
  - JS console logs have `:console-api` for `:source` field.
  - Entries about errors will have SEVERE level.

  * PhantomJS:
  - Return all recorded logs since the last URL change.
  - Does not clear recorded logs on subsequent invocations.
  - JS console logs have nil for `:source` field.
  - Entries about errors will have WARNING level, as coded here:
      https://github.com/detro/ghostdriver/blob/be7ffd9d47c1e76c7bfa1d47cdcde9164fd40db8/src/session.js#L494
"
  {:arglists '([driver])}
  dispatch-driver)

(defmethods get-logs
  [:chrome :phantom]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :log]
    {:type :browser}
    result
    (mapv process-log (:value result))))

(defn supports-logs?
  "Checks whether a driver supports getting console logs."
  [driver]
  (implemented? driver get-logs))

(defn- dump-logs
  [logs filename & [opt]]
  (generate-stream
   logs
   (io/writer filename)
   (merge {:pretty true} opt)))

;;
;; get/set hash
;;

(defn- split-hash [url]
  (str/split url #"#" 2))

(defn set-hash
  "Sets a new hash fragment for the current page.
  Don't include the leading # symbol. Useful when navigating
  on single page applications."
  [driver hash]
  (let [[url _] (split-hash (get-url driver))
        new (format "%s#%s" url hash)]
    (go driver new)))

(defn get-hash
  "Returns the current hash fragment (nil when not set)."
  [driver]
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
;; locators TODO: drop or refactor
;;

(defn use-locator [driver locator]
  (swap! driver assoc :locator locator)
  driver)

(defn use-xpath [driver]
  (use-locator driver locator-xpath))

(defn use-css [driver]
  (use-locator driver locator-css))

(defmacro with-locator [driver locator & body]
  `(let [old# (-> ~driver deref :locator)]
     (swap! ~driver assoc :locator ~locator)
     (try
       ~@body
       (finally
         (swap! ~driver assoc :locator old#)))))

(defmacro with-xpath [driver & body]
  `(with-locator ~driver locator-xpath
     ~@body))

(defmacro with-css [driver & body]
  `(with-locator ~driver locator-css
     ~@body))

;;
;; alerts
;;

(defmulti get-alert-text
  "Returns a string of text that appears in alert dialog (if present)."
  dispatch-driver)

(defmethod get-alert-text :firefox
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :alert :text]
    nil
    resp
    (:value resp)))

(defmethods get-alert-text
  [:chrome :safari]
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :alert_text]
    nil
    resp
    (:value resp)))

(defmulti dismiss-alert
  "Simulates cancelling an alert dialog (pressing cross button)."
  dispatch-driver)

(defmethod dismiss-alert :firefox
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :alert :dismiss]
    nil _))

(defmethods dismiss-alert
  [:chrome :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :dismiss_alert]
    nil _))

(defmulti accept-alert
  "Simulates submitting an alert dialog (pressing OK button)."
  dispatch-driver)

(defmethod accept-alert :firefox
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :alert :accept]
    nil _))

(defmethods accept-alert
  [:chrome :safari]
  [driver]
  (with-resp driver :post
    [:session (:session @driver) :accept_alert]
    nil _))

;;
;; network
;;

(defn connectable?
  "Checks whether it's possible to connect a given host/port pair."
  [host port]
  (with-exception ConnectException false
    (let [socket (java.net.Socket. host port)]
      (if (.isConnected socket)
        (do
          (.close socket)
          true)
        false))))

(defn running?
  "Check whether a driver runs HTTP server."
  [driver]
  (connectable? (:host @driver)
                (:port @driver)))

(defn discover-port ;; todo move to utils
  "Finds a port for a driver type.

  Takes a default one from `defaults` map. If it's already taken,
  continues to take random ports until if finds non-busy one.

  Arguments:

  - `type`: a keyword, browser type (:chrome, :firefox, etc),

  - `host`: a string, hostname or IP.

  Returns a port as an integer."
  [type host]
  (loop [port (or (get-in defaults [type :port])
                  (util/random-port))]
    (if (connectable? host port)
      (recur (util/random-port))
      port)))

;;
;; predicates
;;

(defn driver? [driver type]
  (= (dispatch-driver driver) type))

(defn chrome?
  "Returns true if a driver is a Chrome instance."
  [driver]
  (driver? driver :chrome))

(defn firefox?
  "Returns true if a driver is a Firefox instance."
  [driver]
  (driver? driver :firefox))

(defn phantom?
  "Returns true if a driver is a Phantom.js instance."
  [driver]
  (driver? driver :phantom))

(defn safari?
  "Returns true if a driver is a Safari instance."
  [driver]
  (driver? driver :safari))

(defn headless?
  "Returns true if a driver is run in headless mode (without UI window)."
  [driver]
  (drv/is-headless? @driver))

(defn exists?
  "Returns true if an element exists on the page.

  Keep in mind it does not validates whether the element is visible,
  clickable and so on."
  [driver q]
  (with-http-error
    (get-element-text driver q)
    true))

(def ^{:doc "Oppsite to `exists?`."}
  absent? (complement exists?))

(defmulti displayed-el?
  "Checks whether an element is displayed by its identifier.

  Note: Safari does not have native `displayed` implementation, we
  have to check some common cases manually (CSS display, visibility,
  etc).

  Returns true or false."
  dispatch-driver)

(defmethod displayed-el? :default
  [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :displayed]
    nil
    resp
    (:value resp)))

(defmethod displayed-el? :safari
  [driver el]
  (cond
    (= (get-element-css-el driver el :display)
       "none")
    false
    (= (get-element-css-el driver el :visibility)
       "hidden")
    false
    :else true))

(defn displayed?
  "Checks whether an element is displayed an screen."
  [driver q]
  (displayed-el? driver (query driver q)))

(defn visible?
  "Checks whether an element is visible on the page."
  [driver q]
  (and (exists? driver q)
       (displayed? driver q)))

(def ^{:doc "Oppsite to `visible?`."}
  invisible? (complement visible?))

(defn enabled-el? [driver el]
  (with-resp driver :get
    [:session (:session @driver) :element el :enabled]
    nil
    resp
    (:value resp)))

(defn enabled?
  "Checks whether an element is enabled."
  [driver q]
  (enabled-el? driver (query driver q)))

(def disabled? (complement enabled?))

(defn has-text?
  "Returns true if a passed text appears anywhere on a page.
  With a leading query expression, finds a text inside the first
  element that matches the query."
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
  [driver el class]
  (let [classes (get-element-attr-el driver el "class")]
    (cond
      (nil? classes) false
      (string? classes)
      (str/includes? classes (name class)))))

(defn has-class?
  "Checks whether an element has a specific class."
  [driver q class]
  (has-class-el? driver (query driver q) class))

(def ^{:doc "Opposite to `has-class?`."}
  has-no-class? (complement has-class?))

(defn has-alert?
  "Checks if there is an alert dialog opened on the page."
  [driver]
  (with-http-error
    (get-alert-text driver)
    true))

(def ^{:doc "Opposite to `has-alert?`."}
  has-no-alert? (complement has-alert?))

;;
;; wait functions
;;

(def default-timeout 20)
(def default-interval 0.33)

(defn wait
  "Sleeps for N seconds."
  ([driver sec]
   (wait sec))
  ([sec]
   (Thread/sleep (* sec 1000))))

(defmacro with-wait
  "Executes the body waiting for n seconds before each form.
  Returns a value of the last form. Use that macros to perform
  a bunch of actions slowly. Some SPA applications need extra time
  to re-render the content."
  [n & body]
  `(do ~@(interleave (repeat `(wait ~n)) body)))

(defmacro doto-wait
  "The same as doto but prepends each form with (wait n) clause."
  [n obj & body]
  `(doto ~obj
     ~@(interleave (repeat `(wait ~n)) body)))

(defn wait-predicate
  "Sleeps continuously calling a predicate until it returns true.
  Rises a slingshot exception when timeout is reached.

  Arguments:

  - `pred`: a zero-argument predicate to call;
  - `opt`: a map of optional parameters:
  -- `:timeout` wait limit in seconds, 20 by default;
  -- `:interval` how long to wait b/w calls, 0.33 by default;
  -- `:message` a message that becomes a part of exception when timeout is reached."

  ([pred]
   (wait-predicate pred {}))
  ([pred opt]
   (let [timeout (get opt :timeout default-timeout) ;; refactor this (call for java millisec)
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
     (when-not (with-http-error
                 (pred))
       (wait interval)
       (recur pred (assoc
                    opt
                    :time-rest (- time-rest interval)
                    :times (inc times)))))))

(defn wait-exists
  "Waits until an element exists on a page (but may not be visible though).

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (wait-predicate #(exists? driver q) opt))

(defn wait-absent
  "Waits until an element is absent.

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (let [message (format "Wait for %s element is absent" q)]
    (wait-predicate #(absent? driver q)
                    (assoc opt :message message))))

(defn wait-visible
  "Waits until an element presents and is visible.

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (let [message (format "Wait for %s element is visible" q)]
    (wait-predicate #(visible? driver q)
                    (assoc opt :message message))))

(defn wait-invisible
  "Waits until an element presents but not visible.

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (wait-predicate #(invisible? driver q) opt))

(defn wait-enabled
  "Waits until an element is enabled (usually an input element).

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (wait-predicate #(enabled? driver q) opt))

(defn wait-disabled
  "Waits until an element is disabled (usually an input element).

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (wait-predicate #(disabled? driver q) opt))

(defn wait-has-alert
  "Waits until an alert dialog appears on the screen.

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver & [opt]]
  (wait-predicate #(has-alert? driver) opt))

(defn wait-has-text
  "Waits until an element has text anywhere inside it (including inner HTML).

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `text`: a string to search;
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q text & [opt]]
  (let [message (format "Wait for %s element has text %s"
                        q text)]
    (wait-predicate #(has-text? driver q text)
                    (assoc opt :message message))))

(defn wait-has-class
  "Waits until an element has specific class.

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `class`: a class to search as string;
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q class & [opt]]
  (wait-predicate #(has-class? driver q class) opt))

(defn wait-running [driver & [opt]]
  (log/debugf "Waiting for %s:%s is running"
              (:host @driver) (:port @driver))
  (wait-predicate #(running? driver) opt))

;;
;; visible actions
;;

(defn click-visible
  "Waits until an element becomes visible, then clicks on it.

  Arguments:

  - `driver`: a driver instance;
  - `q`: a query term (see `query`);
  - `opt`: a map of options (see `wait-predicate`)."

  [driver q & [opt]]
  (doto driver
    (wait-visible q opt)
    (click q)))

;;
;; touch
;;

(defmulti touch-tap dispatch-driver)

(defmethod touch-tap
  :chrome
  [driver q]
  (with-resp driver :post
    [:session (:session @driver) :touch :click]
    {:element (query driver q)} _))

(defmulti touch-down dispatch-driver)

(defmethod touch-down
  :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-down driver x y)))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :touch :down]
     {:x (int x) :y (int y)} _)))

(defmulti touch-up dispatch-driver)

(defmethod touch-up
  :chrome
  ([driver q]
   (let [{:keys [x y]}
         (get-element-location driver q)]
     (touch-down driver x y)))
  ([driver x y]
   (with-resp driver :post
     [:session (:session @driver) :touch :up]
     {:x (int x) :y (int y)} _)))

(defmulti touch-move dispatch-driver)

(defmethod touch-move
  :chrome
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

(defmacro when-not-chrome
  "Executes the body only if a browser is NOT Chrome."
  [driver & body]
  `(when-not-predicate #(chrome? ~driver) ~@body))

(defmacro when-not-phantom
  "Executes the body only if a browser is NOT Phantom.js."
  [driver & body]
  `(when-not-predicate #(phantom? ~driver) ~@body))

(defmacro when-not-firefox
  "Executes the body only if a browser is NOT Firefox."
  [driver & body]
  `(when-not-predicate #(firefox? ~driver) ~@body))

(defmacro when-not-safari
  "Executes the body only if a browser is NOT Safari."
  [driver & body]
  `(when-not-predicate #(safari? ~driver) ~@body))

(defmacro when-not-headless
  "Executes the body only if a browser is NOT run in headless mode."
  [driver & body]
  `(when-not-predicate #(headless? ~driver) ~@body))

(defmacro when-predicate
  "Executes the body only if a predicate returns true."
  [predicate & body]
  `(when (~predicate)
     ~@body))

(defmacro when-chrome
  "Executes the body only if the driver is Chrome.

  Example:

  (def driver (chrome))
  (when-chrome driver
    (println \"It's Chrome!\")"

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

(defmacro when-headless
  "Executes the body only if the driver is run in headless mode."
  [driver & body]
  `(when-predicate #(headless? ~driver) ~@body))

;;
;; input
;;

(defn- make-input* [text & more]
  (mapv str (apply str text more)))

(defmulti fill-el
  "Fills an element with text by its identifier."
  {:arglists '([driver el text & more])}
  dispatch-driver)

(defmethod fill-el
  :default
  [driver el text & more]
  (with-resp driver :post
    [:session (:session @driver) :element el :value]
    {:value (apply make-input* text more)} _))

(defmethod fill-el
  :firefox ;; todo support the old version for :default
  [driver el text & more]
  (with-resp driver :post
    [:session (:session @driver) :element el :value]
    {:text (str/join (apply make-input* text more))} _))

(defmulti fill-active*
  {:arglists '([driver text & more])}
  dispatch-driver)

(defmethod fill-active*
  :chrome
  [driver text & more]
  (with-resp driver :post
    [:session (:session @driver) :keys]
    {:value (apply make-input* text more)} _))

(defmethod fill-active*
  :firefox
  [driver text & more]
  (let [el (get-active-element* driver)]
    (apply fill-el driver el text more)))

(defn fill-active
  "Fills an active element with keys."
  [driver text & more]
  (apply fill-active* driver text more))

(defn fill
  "Fills an element found with a query with a given text.

  0.1.6: now the rest parameters are supported. They will
  joined using \"str\":

  (fill driver :simple-input \"foo\" \"baz\" 1)
  ;; fills the input with  \"foobaz1\""
  [driver q text & more]
  (apply fill-el driver (query driver q) text more))

(defn fill-multi
  "Fills multiple inputs in batch.

  `q-text` could be:

  - a map of {query -> text}
  - a vector of [query1 text1 query2 text2 ...]"
  [driver q-text]
  (cond
    (map? q-text)
    (doseq [[q text] q-text]
      (fill driver q text))

    (vector? q-text)
    (recur driver (apply hash-map q-text))

    :else (throw+ {:type :etaoin/argument
                   :message "Wrong argument type"
                   :arg q-text})))

(defn fill-human-el
  ;; todo opt params
  [driver el text]
  (let [mistake-prob 0.1
        pause-max 0.2
        rand-char #(-> 26 rand-int (+ 97) char)
        wait-key #(let [r (rand)]
                    (wait (if (> r pause-max) pause-max r)))]
    (doseq [key text]
      (when (< (rand) mistake-prob)
        (fill-el driver el (rand-char))
        (wait-key)
        (fill-el driver el keys/backspace)
        (wait-key))
      (fill-el driver el key)
      (wait-key))))

(defn fill-human
  "Fills text like humans do: with error, corrections and pauses.

  Arguments:

  - `driver`: a driver instance,

  - `q`: a query term, see `query` function for more info,

  - `text`: a string to input."
  [driver q text]
  (fill-human-el driver (query driver q) text))

(defn clear-el
  "Clears an element by its identifier."
  [driver el]
  (with-resp driver :post
    [:session (:session @driver) :element el :clear]
    nil _))

(defn clear
  "Clears an element (input, textarea) found with a query.

  0.1.6: multiple queries added."
  [driver q & more]
  (doseq [q (cons q more)]
    (clear-el driver (query driver q))))

;;
;; file upload
;;

(defmulti upload-file
  "Attaches a local file to a file input field.

  Arguments:

  - `q` is a query term that refers to a file input;
  - `file` is either a string or java.io.File object
  that references a local file. The file should exist.

  Under the hood, it sends the file's name as a sequence of keys
  to the input."
  (fn [driver q file]
    (type file)))

(defmethod upload-file String
  [driver q path]
  (upload-file driver q (io/file path)))

(defmethod upload-file java.io.File
  [driver q file]
  (let [path (.getAbsolutePath file)
        message (format "File %s does not exist" path)]
    (if (.exists file)
      (fill driver q path)
      (throw+ {:type :etaoin/file
               :message message
               :driver @driver}))))

;;
;; submit
;;

(defn submit
  "Sends Enter button value to an element found with query."
  [driver q]
  (fill driver q keys/enter))

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
  (with-resp driver :post
    [:session (:session @driver) :timeouts]
    {type (util/sec->ms sec)} _))

(defmethod set-timeout*
  :chrome
  [driver type sec]
  (with-resp driver :post
    [:session (:session @driver) :timeouts]
    {:type type :ms (util/sec->ms sec)} _))

(defmulti set-script-timeout
  "Sets timeout for executing JS sctipts."
  {:arglists '([driver sec])}
  dispatch-driver)

(defmethod set-script-timeout
  :default
  [driver sec]
  (set-timeout* driver :script sec))

(defmulti set-page-load-timeout
  "Sets timeout for loading pages."
  {:arglists '([driver sec])}
  dispatch-driver)

(defmethod set-page-load-timeout
  :default
  [driver sec]
  (set-timeout* driver :pageLoad sec))

(defmethod set-page-load-timeout
  :chrome
  [driver sec]
  (set-timeout* driver "page load" sec))

(defmulti set-implicit-timeout
  "Sets timeout that is used when finding elements on the page."
  {:arglists '([driver sec])}
  dispatch-driver)

(defmethod set-implicit-timeout
  :default
  [driver sec]
  (set-timeout* driver :implicit sec))

(defmulti ^:private get-timeout*
  "Basic method to get a map of all the timeouts."
  {:arglists '([driver])}
  dispatch-driver)

(defmethod get-timeout*
  :default
  [driver]
  (with-resp driver :get
    [:session (:session @driver) :timeouts]
    nil
    resp
    (:value resp)))

(defn get-script-timeout
  "Returns the current script timeout in seconds."
  [driver]
  (-> driver get-timeout* :script util/ms->sec))

(defn get-page-load-timeout
  "Returns the current page load timeout in seconds."
  [driver]
  (-> driver get-timeout* :pageLoad util/ms->sec))

(defn get-implicit-timeout
  "Returns the current implicit timeout in seconds."
  [driver]
  (-> driver get-timeout* :implicit util/ms->sec))

(defmacro with-script-timeout
  "Performs the body setting the script timeout temporary.
  Useful for async JS scripts."
  [driver sec & body]
  `(let [prev# (get-script-timeout ~driver)]
     (set-script-timeout ~driver ~sec)
     (try
       ~@body
       (finally
         (set-script-timeout ~driver prev#)))))

;;
;; screenshots
;;

(defmulti ^:private b64-to-file
  "Dumps a Base64-encoded string into a file.
  A file might be either a path or a `java.io.File` instance."
  {:arglists '([b64str file] [b64str filepath])}
  util/dispatch-types)

(defmethod b64-to-file
  [String java.io.File]
  [b64str file]
  (b64-to-file b64str (.getAbsolutePath file)))

(defmethod b64-to-file
  [String String]
  [b64str filepath]
  (with-open [out (io/output-stream filepath)]
    (.write out (-> b64str .getBytes b64/decode))))

(defmulti screenshot
  "Takes a screenshot of the current page. Saves it in a *.png file on disk.
  Rises exception if a screenshot was empty.

  Arguments:

  - `driver`: driver instance,

  - `file`: either a path to a file or a native `java.io.File` instance.
"
  {:arglists '([driver file])}
  dispatch-driver)

(defmethod screenshot :default
  [driver file]
  (with-resp driver :get
    [:session (:session @driver) :screenshot]
    nil
    resp
    (if-let [b64str (-> resp :value not-empty)]
      (b64-to-file b64str file)
      (util/error "Empty screenshot"))))

(defmulti screenshot-element

  {:arglists '([driver q file])}
  dispatch-driver)

(defmethod screenshot-element
  :default
  [driver q file]
  (util/error "This driver doesn't support screening elements."))

(defmethod screenshot-element
  :firefox
  [driver q file]
  (let [el (query driver q)]
    (with-resp driver :get
      [:session (:session @driver) :element el :screenshot]
      nil
      resp
      (if-let [b64str (-> resp :value not-empty)]
        (b64-to-file b64str file)
        (util/error "Empty screenshot, query: %s" q)))))

;;
;; postmortem
;;

(defn get-pwd []
  (System/getProperty "user.dir"))

(defn join-path
  "Joins two and more path components into a single file path OS-wisely."
  [p1 p2 & more]
  (.getPath (apply io/file p1 p2 more)))

(defn format-date
  [date pattern]
  (.format (SimpleDateFormat. pattern) date))

(defn postmortem-handler
  "Internal postmortem handler that creates files.
  See the `with-postmortem`'s docstring below for more info."
  [driver {:keys [dir dir-src dir-img dir-log date-format]}]
  (let [dir     (or dir (get-pwd))
        dir-img (or dir-img dir)
        dir-src (or dir-src dir)
        dir-log (or dir-log dir)

        file-tpl "%s-%s-%s-%s.%s"

        date-format (or date-format "yyyy-MM-dd-hh-mm-ss")
        params [(-> @driver :type name)
                (-> @driver :host)
                (-> @driver :port)
                (format-date (Date.) date-format)]

        file-img (apply format file-tpl (conj params "png"))
        file-src (apply format file-tpl (conj params "html"))
        file-log (apply format file-tpl (conj params "json"))

        path-img (join-path dir-src file-img)
        path-src (join-path dir-src file-src)
        path-log (join-path dir-src file-log)]

    (log/debugf "Writing screenshot: %s" path-img)
    (screenshot driver path-img)

    (log/debugf "Writing HTML source: %s" path-src)
    (spit path-src (get-source driver))

    (when (supports-logs? driver)
      (log/debugf "Writing console logs: %s" path-log)
      (dump-logs (get-logs driver) path-log))))

(defmacro with-postmortem
  "Wraps the body with postmortem handler. If any error occurs,
  it will save a screenshot, the page's source code and console logs
  (if supported) on disk before rising an exception. Having them
  could help you to discover what happened.

  Note: do not use it in test's fixtures. The standard `clojure.test`
  framework has its own way of handling exceptions, so wrapping a fixture
  with `(with-postmortem...)` would be in vain.

  Arguments:

  - `driver`: a driver instance,

  - `opt`: a map of options, where:

  -- `:dir` path to a directory where to store artifacts by default.
  When not passed, the current working directory (`pwd`) is used.

  -- `:dir-img`: path to a directory where to store `.png`
  files (screenshots). If `nil`, `:dir` value is used.

  -- `:dir-src`: path to a directory where to store `.html`
  files (page source). If `nil`, `:dir` value is used.

  -- `:dir-log`: path to a directory where to store `.json`
  files with console logs. If `nil`, `:dir` value is used.

  -- `:date-format`: a string represents date(time) pattern to make
  filenames unique. Default is \"yyyy-MM-dd-hh-mm-ss\". See Oracle
  Java `SimpleDateFormat` class manual for more patterns."
  [driver opt & body]
  `(try
     ~@body
     (catch Exception e#
       (postmortem-handler ~driver ~opt)
       (throw e#))))

;;
;; driver management
;;

(defn make-url
  "Makes an Webdriver URL from a host and port."
  [host port]
  (format "http://%s:%s" host port))

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
  from the `defaults` global map if is not passed. If there is no
  port in that map, a random-generated port is used.

  -- `:locator` is a string determs what algorithm to use by default
  when finding elements on a page. `default-locator` variable is used
  if not passed."
  [type & [opt]] ;; todo move port and host to create-driver
  (let [driver (atom {})
        host (or (:host opt) "127.0.0.1")
        port (or (:port opt)
                 (discover-port type host))
        url (make-url host port)
        locator (or (:locator opt) default-locator)]
    (swap! driver assoc
           :type type
           :host host
           :port port
           :url url
           :locator locator)
    (log/debugf "Created driver: %s %s:%s" (name type) host port)
    driver))

(defn run-driver
  "Runs a driver process locally.

  Creates a UNIX process with a Webdriver HTTP server. Host and port
  are taken from a `driver` argument. Updates a driver instance with
  new fields with process information. Returns modified driver.

  Arguments:

  - `driver` is an atom created with `create-driver` function.

  - `opt` is an optional map with the following possible parameters:

  -- `:path-driver` is a string path to the driver's binary file. When
  not passed, it is taken from defaults.

  -- `:path-browser` is a string path to the browser's binary
  file. When not passed, the driver discovers it by its own.

  -- `:size` is a vector of two integers specifying initial window size.

  -- `:url` is a string with the default URL opened by default (FF only for now).

  -- `:log-level` a keyword to set browser's log level. Used when fetching
  browser's logs. Possible values are: `:off`, `:debug`, `:warn`, `:info`,
  `:error`, `:all`. When not passed, `:all` is set.

  -- `:profile` is a string path that points on profile folder.
  See the `Setting browser profile` section in `README.md` to know
  how to do it properly.

  -- `headless` is a boolean flag to run the browser in headless mode
  (i.e. without GUI window). Useful when running tests on CI servers
  rather than local machine. Currently, only FF and Chrome support headless mode.
  Phantom.js is headless by its nature.

  -- `:args` is a vector of additional command line arguments
  to the browser's process.

  -- `:prefs` is a map of browser-specific preferences.

  -- `:args-driver` is a vector of additional arguments to the
  driver's process.

  -- `:env` is a map with system ENV variables. Keys are turned into
  upper-case strings."
  ;; todo: get rid of atom storage
  ;; todo: quite ugly
  [driver & [{:keys [env
                     url
                     args
                     size
                     prefs
                     profile
                     headless
                     log-level
                     args-driver
                     path-driver
                     download-dir
                     path-browser]}]]
  (let [{:keys [type port]} @driver
        [with height] size
        log-level (or log-level :all)
        path-driver (or path-driver (get-in defaults [type :path]))
        _ (swap! driver drv/set-browser-log-level log-level)
        _ (swap! driver drv/set-path path-driver)
        _ (swap! driver drv/set-port port)
        _ (when args-driver (swap! driver drv/set-args args-driver))
        _ (when size (swap! driver drv/set-window-size with height))
        _ (when url (swap! driver drv/set-url url))
        _ (when headless (swap! driver drv/set-headless))
        _ (when args (swap! driver drv/set-options-args args))
        _ (when profile (swap! driver drv/set-profile profile))
        _ (when path-browser (swap! driver drv/set-binary path-browser))
        _ (when download-dir (swap! driver drv/set-download-dir download-dir))
        _ (when prefs (swap! driver drv/set-prefs prefs))
        proc-args (drv/get-args @driver)
        _ (log/debugf "Starting process: %s" (str/join \space proc-args))
        process (proc/run proc-args)]
    (swap! driver assoc
           :env env  ;; todo process env
           :process process)
    driver))

(defn connect-driver
  "Connects to a running Webdriver server.

  Creates a new session on Webdriver HTTP server. Sets the session to
  the driver. Returns the modified driver.

  Arguments:

  - `opt`: an map of the following optional parameters:

  -- `:capabilities` a map of desired capabilities your
  browser should support;

  -- `:desired-capabilities`: an alias for `:capabilities`.

  See https://www.w3.org/TR/webdriver/#capabilities"
  [driver & [opt]] ;; move params here
  (wait-running driver)
  (let [type (:type @driver)
        _ (swap! driver drv/set-capabilities (get-in defaults [type :capabilities]))
        _ (swap! driver drv/set-capabilities (:capabilities opt))
        _ (swap! driver drv/set-capabilities (:desired-capabilities opt))
        caps (:capabilities @driver)
        session (create-session driver caps)]
    (swap! driver assoc :session session)
    driver))

(defn disconnect-driver
  "Disconnects from a running Webdriver server.

  Closes the current session that is stored in the driver. Removes the
  session from the driver instance. Returns modified driver."
  [driver]
  (delete-session driver)
  (swap! driver dissoc
         :session :capabilities)
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
  `run-driver` and `connect-driver` may accept."
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

(defn chrome-headless
  "Launches headless Chrome driver. A shortcut for `boot-driver`."
  [& opt]
  (boot-driver :chrome (assoc opt :headless true)))

(defn firefox-headless
  "Launches headless Firefox driver. A shortcut for `boot-driver`."
  [& opt]
  (boot-driver :firefox (assoc opt :headless true)))

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

(defmacro with-chrome-headless
  "Performs the body with headless Chrome session. A shortcut for
  `with-driver`."
  [opt bind & body]
  `(with-driver :chrome (assoc ~opt :headless true) ~bind
     ~@body))

(defmacro with-firefox-headless
  "Performs the body with headless Firefox session. A shortcut for
  `with-driver`."
  [opt bind & body]
  `(with-driver :firefox (assoc ~opt :headless true) ~bind
     ~@body))
