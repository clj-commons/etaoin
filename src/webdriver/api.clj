(ns webdriver.api
  "
  Documentation? Read the code, Luke!

  Chrome:
  https://github.com/bayandin/chromedriver/blob/e9a1f55b166ea62ef0f6e78da899d9abf117e88f/client/command_executor.py

  Firefox (Geckodriver):
  https://github.com/mozilla/webdriver-rust/blob/7ec65451c99b638655c72e7b9718a374ff60de87/src/httpapi.rs

  Phantom.js (Ghostdriver)
  https://github.com/detro/ghostdriver/blob/873c9d660a80a3faa743e4f352571ce4559fe691/src/request_handlers/session_request_handler.js
  https://github.com/detro/ghostdriver/blob/873c9d660a80a3faa743e4f352571ce4559fe691/src/request_handlers/webelement_request_handler.js
  "
  (:require [webdriver.client :as client]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [slingshot.slingshot :refer [throw+]]))

;;
;; defaults
;;

(def default-capabilities
  {:browserName "*"
   :browserVersion "*"
   :platformName "*"
   :platformVersion "*"
   :acceptInsecureCerts false
   :javascriptEnabled true})

;;
;; helpers
;;

(defn text-to-array [text]
  (cond
    (char? text) [text]
    :else (vec text)))

(defn bool? [val]
  (or (true? val) (false? val)))

(defn b64-to-file [b64str filename]
  (with-open [out (io/output-stream filename)]
    (.write out (-> b64str
                    .getBytes
                    b64/decode))))

(defn check-screenshot [value context]
  (if (empty? value)
    (throw+ (assoc context :type :webdriver/empty-screenshot))
    value))

;;
;; api
;;

(defn new-session
  "https://www.w3.org/TR/webdriver/#dfn-new-session"
  [server cap-desired cap-required]
  {:pre [(map? server) (map? cap-desired) (map? cap-required)]
   :post [(string? %)]}
  (let [meth :post
        path [:session]
        body {:desiredCapabilities (merge default-capabilities cap-desired)
              :requiredCapabilities cap-required}
        resp (client/call server meth path body)]
    (:sessionId resp)))

(defn delete-session
  "https://www.w3.org/TR/webdriver/#dfn-delete-session"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [meth :delete
        path [:session session]
        body {}
        resp (client/call server meth path body)]
    (-> resp :value)))

(defn status
  "https://www.w3.org/TR/webdriver/#dfn-status"
  [server]
  {:pre [(map? server)]
   :post [(map? %)]}
  (let [meth :get
        path [:status]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn go [server session url]
  "https://www.w3.org/TR/webdriver/#dfn-go"
  (let [meth :post
        path [:session session :url]
        body {:url url}
        resp (client/call server meth path body)]
    (-> resp :value)))

(defn get-current-url
  "https://www.w3.org/TR/webdriver/#dfn-get-current-url"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :url]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn back
  "https://www.w3.org/TR/webdriver/#dfn-back"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [meth :post
        path [:session session :back]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn forward
  "https://www.w3.org/TR/webdriver/#dfn-forward"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [meth :post
        path [:session session :forward]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn refresh
  "https://www.w3.org/TR/webdriver/#dfn-refresh"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [meth :post
        path [:session session :refresh]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-title
  "https://www.w3.org/TR/webdriver/#dfn-get-title"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :title]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-window-handle-FF [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (-> server
      (client/call :get [:session session :window])
      :value))

(defn close-window
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [meth :delete
        path [:session session :window]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn switch-to-window
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-window"
  [server session handle]
  {:pre [(map? server) (string? session) (string? handle)]}
  (let [meth :post
        path [:session session :window]
        body {:handle handle}
        resp (client/call server meth path body)]
    (-> resp :value)))

(defn get-window-handles-FF
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handles"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(vector %)]}
  (let [meth :get
        path [:session session :window :handles]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-window-size
  "todo"
  [server session handle]
  {:pre [(map? server) (string? session) (string? handle)]
   :post [(map? %)]}
  (let [meth :get
        path [:session session :window handle :size]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-window-position
  "todo"
  [server session handle]
  {:pre [(map? server) (string? session) (string? handle)]
   :post [(map? %)]}
  (let [meth :get
        path [:session session :window handle :position]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn set-window-position
  "todo"
  [server session handle x y]
  {:pre [(map? server) (string? session) (string? handle)]}
  (let [meth :post
        path [:session session :window handle :position]
        data {:x x :y y}
        resp (client/call server meth path data)]
    (-> resp :value)))

(defn set-window-size
  "todo"
  [server session handle width height]
  {:pre [(map? server) (string? session) ]
}
  (let [meth :post
        path [:session session :window handle :size]
        body {:width width :height height}
        resp (client/call server meth path body)]
    (-> resp :value)))

(defn get-window-handle
  "todo"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :window_handle]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-window-handles
  "todo"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(vector? %)]}
  (let [meth :get
        path [:session session :window_handles]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-window-size-FF
  "todo"
  [server session]
  {:pre [(map? server) (string? session) ]
   }
  (let [meth :get
        path [:session session :window :size]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn set-window-size-FF
  "todo"
  [server session width height]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [meth :post
        path [:session session :window :position]
        body {:width width :height height}
        resp (client/call server meth path body)]))

(defn set-window-position-FF
  "todo"
  [server session x y]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [meth :post
        path [:session session :window :position]
        body {:x x :y y}
        resp (client/call server meth path body)]))

(defn maximize-window-FF
  "https://www.w3.org/TR/webdriver/#dfn-maximize-window"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [meth :post
        path [:session session :window :maximize]
        resp (client/call server meth path)]))

(defn fullscreen-window-FF
  "https://www.w3.org/TR/webdriver/#dfn-fullscreen-window"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [meth :post
        path [:session session :window :fullscreen]
        resp (client/call server meth path)]))

(defn get-active-element
  "https://www.w3.org/TR/webdriver/#dfn-get-active-element"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :element :active]
        resp (client/call server meth path)
        parser #(case (:browser server)
                  (:chrome :phantom) (-> % :ELEMENT)
                  (-> % first second))]
    (-> resp :value parser)))

(defn find-element
  "https://www.w3.org/TR/webdriver/#dfn-find-element"
  [server session locator term]
  {:pre [(map? server) (string? session) (string? locator) (string? term)]
   :post (string? %)}
  (let [meth :post
        path [:session session :element]
        body {:using locator :value term}
        resp (client/call server meth path body)
        parser #(case (:browser server)
                  (:chrome :phantom) (-> % :ELEMENT)
                  (-> % first second))]
    (-> resp :value parser)))

(defn find-elements
  "https://www.w3.org/TR/webdriver/#dfn-find-elements"
  [server session locator term]
  {:pre [(map? server) (string? session) (string? locator) (string? term)]
   :post (vector? %)}
  (let [meth :post
        path [:session session :elements]
        body {:using locator :value term}
        resp (client/call server meth path body)
        parser #(case (:browser server)
                  (:chrome :phantom) (-> % :ELEMENT)
                  (-> % first second))]
    (->> resp :value (mapv parser))))

(defn find-element-from-element
  "https://www.w3.org/TR/webdriver/#dfn-find-element-from-element"
  [server session element locator term]
  {:pre [(map? server) (string? session) (string? locator) (string? term)]
   :post (string? %)}
  (let [meth :post
        path [:session session :element element :element]
        body {:using locator :value term}
        resp (client/call server meth path body)
        parser #(case (:browser server)
                  (:chrome :phantom) (-> % :ELEMENT)
                  (-> % first second))]
    (-> resp :value parser)))

(defn find-elements-from-element
  "https://www.w3.org/TR/webdriver/#dfn-find-elements-from-element"
  [server session element locator term]
  {:pre [(map? server) (string? session) (string? locator) (string? term)]
   :post (vector? %)}
  (let [meth :post
        path [:session session :element element :elements]
        body {:using locator :value term}
        resp (client/call server meth path body)
        parser #(case (:browser server)
                  (:chrome :phantom) (-> % :ELEMENT)
                  (-> % first second))]
    (->> resp :value (mapv parser))))

(defn is-element-displayed
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(bool? %)]}
  (let [meth :get
        path [:session session :element element :displayed]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn is-element-selected
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(bool? %)]}
  (let [meth :get
        path [:session session :element element :selected]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-element-attribute
  "https://www.w3.org/TR/webdriver/#dfn-get-element-attribute"
  [server session element attribute]
  {:pre [(map? server) (string? session) (string? element) (string? attribute)]
   :post [(or (string? %) (nil? %))]}
  (let [meth :get
        path [:session session :element element :attribute attribute]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-element-property
  "https://www.w3.org/TR/webdriver/#dfn-get-element-property"
  [server session element property]
  {:pre [(map? server) (string? session) (string? element) (string? property)]
   :post [(or (string? %) (nil? %))]}
  (let [meth :get
        path [:session session :element element :property property]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-element-css-value
  "https://www.w3.org/TR/webdriver/#dfn-get-element-css-value"
  [server session element property]
  {:pre [(map? server) (string? session) (string? element) (string? property)]
   :post [(or (string? %) (nil? %))]}
  (let [meth :get
        path [:session session :element element :css property]
        resp (client/call server meth path)]
    (-> resp :value not-empty)))

(defn get-element-text
  "https://www.w3.org/TR/webdriver/#dfn-get-element-text"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :element element :text]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-element-value
  "todo"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :element element :value]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-element-tag-name
  "https://www.w3.org/TR/webdriver/#dfn-get-element-tag-name"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(string? %)]}
  (let [meth :get
        path [:session session :element element :name]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn is-element-enabled
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(bool? %)]}
  (let [meth :get
        path [:session session :element element :enabled]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn element-click
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]}
  (let [meth :post
        path [:session session :element element :click]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn element-tap-FF
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(nil? %)]}
  (let [method :post
        url [:session session :element element :tap]
        resp (client/call server method url)]))

(defn element-clear
  "https://www.w3.org/TR/webdriver/#dfn-element-clear"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]}
  (let [method :post
        url [:session session :element element :clear]
        resp (client/call server method url)]
    (-> resp :value)))

(defn element-send-keys
  "https://www.w3.org/TR/webdriver/#dfn-element-send-keys"
  [server session element text]
  {:pre [(map? server) (string? session) (string? element) (string? text)]
   :post [(nil? %)]}
  (let [method :post
        url [:session session :element element :value]
        data {:value (text-to-array text)}
        resp (client/call server method url data)]))

(defn get-page-source
  "https://www.w3.org/TR/webdriver/#dfn-get-page-source"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(string? %)]}
  (let [method :get
        url [:session session :source]
        resp (client/call server method url)]
    (-> resp :value)))

(defn execute-script
  "https://www.w3.org/TR/webdriver/#dfn-execute-script"
  [server session script & args]
  {:pre [(map? server) (string? session) (string? script)]}
  (let [cmd (case (:browser server)
              (:chrome :phantom) :execute
              :sync)
        method :post
        url [:session session cmd]
        data {:script script :args (vec args)}
        resp (client/call server method url data)]
    (-> resp :value)))

(defn get-all-cookies
  "https://www.w3.org/TR/webdriver/#dfn-get-all-cookies"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(vector? %)]}
  (let [method :get
        url [:session session :cookie]
        resp (client/call server method url)]
    (-> resp :value)))

(defn get-named-cookie-FF
  "https://www.w3.org/TR/webdriver/#dfn-get-named-cookie"
  [server session name]
  {:pre [(map? server) (string? session) (string? name)]
   :post [(vector? %)]}

  (case (:browser server)

    (:chrome :phantom)
    (throw+ {:type :webdriver/not-implemented})
    ;; (as->
    ;;   (get-all-cookies server session) $
    ;;   (filter #(-> % :name (= name)) $)
    ;;   (vec $))

    (let [method :get
          url [:session session :cookie name]
          resp (client/call server method url)]
      (-> resp :value))))

(defn add-cookie
  "https://www.w3.org/TR/webdriver/#dfn-add-cookie"
  [server session cookie]
  {:pre [(map? server) (string? session) (map? cookie)]
   :post [(nil? %)]}
  (let [method :post
        url [:session session :cookie]
        data {:cookie cookie}
        resp (client/call server method url data)]))

(defn delete-cookie
  "https://www.w3.org/TR/webdriver/#dfn-delete-cookie"
  [server session name]
  {:pre [(map? server) (string? session) (string? name)]
   :post [(nil? %)]}
  (let [method :delete
        url [:session session :cookie name]
        resp (client/call server method url)]))

(defn delete-all-cookies
  "https://www.w3.org/TR/webdriver/#dfn-delete-all-cookies"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [method :delete
        url [:session session :cookie]
        resp (client/call server method url)]))

(defn ^{:phantom false}
  dismiss-alert
  "https://www.w3.org/TR/webdriver/#dfn-dismiss-alert"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [cmd (case (:browser server)
              :chrome [:dismiss_alert]
              [:alert :dismiss])
        meth :post
        path (into [:session session] cmd)
        resp (client/call server meth path)]
    (-> resp :value)))

(defn
  ^{:phantom false}
  accept-alert
  "https://www.w3.org/TR/webdriver/#dfn-accept-alert"
  [server session]
  {:pre [(map? server) (string? session)]}
  (let [cmd (case (:browser server)
              :chrome [:accept_alert]
              [:alert :accept])
        meth :post
        path (into [:session session] cmd)
        resp (client/call server meth path)]
    (-> resp :value)))

(defn get-alert-text
  "https://www.w3.org/TR/webdriver/#dfn-get-alert-text"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(string? %)]}
  (let [cmd (case (:browser server)
              :chrome [:alert_text]
              [:alert :text])
        meth :get
        path (into [:session session] cmd)
        resp (client/call server meth path)]
    (-> resp :value)))

(defn send-alert-text
  "https://www.w3.org/TR/webdriver/#dfn-send-alert-text"
  [server session text]
  {:pre [(map? server) (string? session) (string? text)]}
  (let [cmd (case (:browser server)
              :chrome [:alert_text]
              [:alert :text])
        meth :post
        path (into [:session session] cmd)
        body {:value (text-to-array text)}
        resp (client/call server meth path body)]
    (-> resp :value)))

(defn take-screenshot
  "https://www.w3.org/TR/webdriver/#dfn-take-screenshot"
  [server session filename]
  {:pre [(map? server) (string? session) (string? filename)]
   :post [(nil? %)]}
  (let [meth :get
        path [:session session :screenshot]
        resp (client/call server meth path)]
    (-> resp
        :value
        (check-screenshot {:server server
                           :filename filename})
        (b64-to-file filename))))

(defn take-element-screenshot
  "https://www.w3.org/TR/webdriver/#dfn-take-element-screenshot"
  [server session element filename]
  {:pre [(map? server) (string? session) (string? element) (string? filename)]
   :post [(nil? %)]}
  (let [meth :get
        path [:session session :element element :screenshot]
        resp (client/call server meth path)]
    (-> resp
        :value
        (check-screenshot {:server server
                           :filename filename})
        (b64-to-file filename))))

;; the functions below don't work in FF

(defn mouse-move-to
  "Moves virtual mouse to an element or by offset."
  [server session & {:keys [element xoffset yoffset] :as payload}]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (case (:browser server)
    ;; :firefox (throw+ {:type :webdriver/not-implemented})
    (let [meth :post
          path [:session session :moveto]
          body payload
          resp (client/call server meth path body)])))

(defn mouse-button-down
  "Set mouse button pressed during the next API calls."
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [meth :post
          path [:session session :buttondown]
          resp (client/call server meth path)]))

(defn mouse-button-up
  "Releases the mouse button."
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(nil? %)]}
  (let [meth :post
          path [:session session :buttonup]
          resp (client/call server meth path)]))

(defn get-element-location
  "todo"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(map? %)]}
  (let [meth :get
        path [:session session :element element :location]
        resp (client/call server meth path)]
    (-> resp :value (select-keys [:x :y]))))

(defn get-element-size
  "todo"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]
   :post [(map? %)]}
  (let [meth :get
        path [:session session :element element :size]
        resp (client/call server meth path)]
    (-> resp :value (select-keys [:width :height]))))
