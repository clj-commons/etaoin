(ns webdriver.api
  "
  Documentation? Read the code, Luke!

  Chrome:
  https://github.com/bayandin/chromedriver/blob/e9a1f55b166ea62ef0f6e78da899d9abf117e88f/client/command_executor.py

  Firefox (Geckodriver):
  https://github.com/mozilla/webdriver-rust/blob/7ec65451c99b638655c72e7b9718a374ff60de87/src/httpapi.rs

  Phantom.js (Ghostdriver)
  https://github.com/detro/ghostdriver/tree/873c9d660a80a3faa743e4f352571ce4559fe691/src/request_handlers
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

(defn session-id [session]
  (-> session :sessionId))

(defn text-to-array [text]
  (cond
    (char? text) [text]
    :else (vec text)))

(defn parse-element [data]
  (-> data :ELEMENT)
  ;; (-> data first second)
  )



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

(defn get-timeout [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-timeout"
  (client/call server :get
               [:session session :timeouts]))

(defn set-timeout [server session type msec]
  "https://www.w3.org/TR/webdriver/#dfn-set-timeouts"
  (client/call server :post
               [:session session :timeouts]
               {:type type :ms msec}))

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

(defn get-window-handle [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (-> server
      (client/call :get [:session session :window])
      :value))

(defn close-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (client/call server :delete [:session session :window]))

(defn switch-to-window [server session handle]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-window"
  (client/call server :post
               [:session session :window]
               {:handle handle}))

(defn get-window-handles [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handles"
  (-> server
      (client/call :get [:session session :window :handles])
      :value))

(defn switch-to-frame [server session frame]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-frame"
  (-> server
      (client/call
       :post [:session session :frame]
       {:id frame})))

(defn switch-to-parent-frame [server session]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-parent-frame"
  (-> server
      (client/call
       :post [:session session :frame :parent])))

(defn get-window-size [server session]
  (-> server
      (client/call :get [:session session :window :size])))

(defn set-window-size [server session width height]
  (-> server
      (client/call :post [:session session :window :size]
                   {:width width :height height})))

(defn set-window-position [server session x y]
  (-> server
      (client/call :post [:session session :window :position]
                   {:x x :y y})))

(defn ^:not-implemented
  get-window-rect [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-rect"
  (-> server
      (client/call :get [:session session :window :rect])))

(defn ^:not-implemented
  set-window-rect [server session x y width height]
  "https://www.w3.org/TR/webdriver/#dfn-set-window-rect"
  (client/call :post
               [:session session :window :rect]
               {:x x :y y :width width :height height}))

(defn maximize-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-maximize-window"
  (client/call server :post
               [:session session :window :maximize]))

(defn ^:not-implemented
  fullscreen-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-fullscreen-window"
  (client/call server :post
               [:session session :window :fullscreen]))

;; ff only
(defn get-active-element [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-active-element"
  (-> (client/call server :get
                   [:session session :element :active])
      :value
      parse-element))

(defn find-element
  "https://www.w3.org/TR/webdriver/#dfn-find-element"
  [server session locator term]
  {:pre [(map? server) (string? session) (string? locator) (string? term)]
   :post (string? %)}
  (let [meth :post
        path [:session session :element]
        body {:using locator :value term}
        resp (client/call server meth path body)]
    (case (:browser server)
      :firefox (-> resp :value first second)
      (:chrome :phantom) (-> resp :value :ELEMENT))))

(defn find-elements [server session locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-elements"
  (-> server
      (client/call :post
                   [:session session :elements]
                   {:using locator :value term})
      :value
      (->> (mapv parse-element))))

(defn find-element-from-element
  "https://www.w3.org/TR/webdriver/#dfn-find-element-from-element"
  [server session element locator term]
  {:pre [(map? server) (string? session) (string? locator) (string? term)]
   :post (string? %)}
  (let [meth :post
        path [:session session :element element :element]
        body {:using locator :value term}
        resp (client/call server meth path body)]
    (case (:browser server)
      :firefox (-> resp :value first second)
      (:chrome :phantom) (-> resp :value :ELEMENT))))

(defn find-elements-from-element [server session element locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-elements-from-element"
  (-> server
      (client/call :post
                   [:session session :element element :elements]
                   {:using locator :value term})
      :value
      (->> (mapv parse-element))))

(defn is-element-displayed [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  (-> server
      (client/call :get
                   [:session session :element element :displayed])
      :value))

(defn is-element-selected [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  (-> server
      (client/call :get
                   [:session session :element element :selected])
      :value))

(defn get-element-attribute [server session element attribute]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-attribute"
  (-> server
      (client/call :get
                   [:session session :element element :attribute attribute])
      :value))

(defn get-element-property [server session element property]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-property"
  (-> server
      (client/call :get
                   [:session session :element element :property property])
      :value))

(defn get-element-css-value [server session element property]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-css-value"
  (-> server
      (client/call :get
                   [:session session :element element :css property])
      :value))

(defn get-element-text [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-text"
  (-> server
      (client/call :get
                   [:session session :element element :text])
      :value))

(defn get-element-tag-name [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-tag-name"
  (-> server
      (client/call :get
                   [:session session :element element :name])
      :value))

(defn
  ^{:chrome false}
  get-element-rect [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-rect"
  (-> server
      (client/call :get
                   [:session session :element element :rect])))

(defn is-element-enabled [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (client/call :get
                   [:session session :element element :enabled])
      :value))

(defn element-click
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  [server session element]
  {:pre [(map? server) (string? session) (string? element)]}
  (let [meth :post
        path [:session session :element element :click]
        resp (client/call server meth path)]
    (-> resp :value)))

(defn ^{:chrome false}
  element-tap [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (client/call :post
                   [:session session :element element :tap])))

(defn element-clear [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-element-clear"
  (-> server
      (client/call :post
                   [:session session :element element :clear])))

(defn element-send-keys
  "https://www.w3.org/TR/webdriver/#dfn-element-send-keys"
  [server session element text]
  {:pre [(map? server) (string? session) (string? element) (string? text)]}
  (let [method :post
        url [:session session :element element :value]
        data {:value (text-to-array text)}
        resp (client/call server method url data)]
    (-> resp :value)))

(defn get-page-source [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-page-source"
  (-> server
      (client/call :get
                   [:session session :source])
      :value))

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

(defn execute-async-script [server session script & args]
  "https://www.w3.org/TR/webdriver/#dfn-execute-async-script"
  (-> server
      (client/call :post
                   [:session session :execute :async]
                   {:script script :args args})))

(defn get-all-cookies
  "https://www.w3.org/TR/webdriver/#dfn-get-all-cookies"
  [server session]
  {:pre [(map? server) (string? session)]
   :post [(vector? %)]}
  (let [method :get
        url [:session session :cookie]
        resp (client/call server method url)]
    (-> resp :value)))

(defn get-named-cookie
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

(defn add-cookie [server session cookie]
  "https://www.w3.org/TR/webdriver/#dfn-add-cookie"
  (-> server
      (client/call :post
                   [:session session :cookie]
                   {:cookie cookie})))

(defn delete-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-delete-cookie"
  (-> server
      (client/call :delete
                   [:session session :cookie name])))

(defn delete-all-cookies [server session]
  "https://www.w3.org/TR/webdriver/#dfn-delete-all-cookies"
  (-> server
      (client/call :delete
                   [:session session :cookie])))

(defn perform-actions [server session actions]
  "https://www.w3.org/TR/webdriver/#dfn-perform-implementation-specific-action-dispatch-steps"
  (-> server
      (client/call :post
                   [:session session :actions]
                   actions)))

(defn release-actions [server session]
  "https://www.w3.org/TR/webdriver/#dfn-release-actions"
  (-> server
      (client/call :delete
                   [:session session :actions])))

(defn
  ^{:firefox false}
  dismiss-alert [server session]
  "https://www.w3.org/TR/webdriver/#dfn-dismiss-alert"
  (-> server
      (client/call :post
                   [:session session :alert :dismiss])))

(defn
  ^{:firefox false}
  accept-alert [server session]
  "https://www.w3.org/TR/webdriver/#dfn-accept-alert"
  (-> server
      (client/call :post
                   [:session session :alert :accept])))

(defn get-alert-text [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-alert-text"
  (-> server
      (client/call :get
                   [:session session :alert :text])
      :value))

(defn send-alert-text [server session text]
  "https://www.w3.org/TR/webdriver/#dfn-send-alert-text"
  (-> server
      (client/call :post
                   [:session session :alert :text]
                   {:value (text-to-array text)})))

(defn take-screenshot [server session filename]
  "https://www.w3.org/TR/webdriver/#dfn-take-screenshot"
  (-> server
      (client/call :get
                   [:session session :screenshot])
      :value
      (check-screenshot {:server server
                         :filename filename})
      (b64-to-file filename)))

(defn take-element-screenshot [server session element filename]
  "https://www.w3.org/TR/webdriver/#dfn-take-element-screenshot"
  (-> server
      (client/call :get
                   [:session session :element element :screenshot])
      :value
      (check-screenshot {:server server
                         :element element
                         :filename filename})
      (b64-to-file filename)))

(defn mouse-move-to
  "Moves virtual mouse."
  [server session & {:keys [element xoffset yoffset] :as payload}]
  {:pre [(map? server) (string? session)]}
  (case (:browser server)
    ;; :firefox (throw+ {:type :webdriver/not-implemented})
    (let [meth :post
          path [:session session :moveto]
          body payload
          resp (client/call server meth path body)]
      (-> resp :value))))
