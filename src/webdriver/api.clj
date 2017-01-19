(ns webdriver.api
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]))

;;
;; todo http timeout
;; global param names
;; client module?
;; todo use as->
;;

;;
;; params
;;

(def default-api-params
  {:as :json
   :accept :json
   :content-type :json
   :form-params {}
   :throw-exceptions true
   ;; :debug true
   })

(def default-capabilities
  {:browserName "firefox"
   :javascriptEnabled true
   :acceptSslCerts true
   :platform "ANY"
   :marionette true
   :name "Sample Test"})

;;
;; tools
;;

(defn url-item-str [item]
  (cond
    (keyword? item) (name item)
    (symbol? item) (name item)
    (string? item) item
    :else (str item)))

(defn get-url-path [items]
  (str/join "/" (map url-item-str items)))

(defn session-id [session]
  (-> session :sessionId))

(defn text-to-array [text]
  (cond
    (char? text) [text]
    :else (vec text)))

(defn parse-element [data]
  (-> data first second))

(defn b64-to-file [b64str filename]
  (with-open [out (io/output-stream filename)]
    (.write out (-> b64str
                    .getBytes
                    b64/decode))))

;;
;; api
;;

(defn api
  ([server method path-args]
   (api server method path-args {}))
  ([server method path-args payload]
   (let [path (get-url-path path-args)
         url (-> server :url (str "/" path))
         params (merge default-api-params
                       {:url url
                        :method method
                        :form-params payload})]
     (-> params
         client/request
         :body))))

(defn new-session [server capabilities]
  "https://www.w3.org/TR/webdriver/#dfn-new-session"
  (api server :post [:session]
       {:desiredCapabilities (merge default-capabilities
                                    capabilities)}))

(defn delete-session [server session]
  "https://www.w3.org/TR/webdriver/#dfn-delete-session"
  (api server
       :delete
       [:session (session-id session)]))

(defn status [server]
  "https://www.w3.org/TR/webdriver/#dfn-status"
  (api server :get [:status]))

(defn get-timeout [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-timeout"
  (api server :get
       [:session (session-id session) :timeouts]))

(defn set-timeout [server session type msec]
  "https://www.w3.org/TR/webdriver/#dfn-set-timeouts"
  (api server :post
       [:session (session-id session) :timeouts]
       {:type type :ms msec}))

(defn go [server session url]
  "https://www.w3.org/TR/webdriver/#dfn-go"
  (api server
       :post
       [:session (session-id session) :url]
       {:url url}))

(defn get-current-url [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-current-url"
  (api server
       :get
       [:session (session-id session) :url]))

(defn back [server session]
  "https://www.w3.org/TR/webdriver/#dfn-back"
  (api server
       :post
       [:session (session-id session) :back]))

(defn forward [server session]
  "https://www.w3.org/TR/webdriver/#dfn-forward"
  (api server
       :post
       [:session (session-id session) :forward]))

(defn refresh [server session]
  "https://www.w3.org/TR/webdriver/#dfn-refresh"
  (api server
       :post
       [:session (session-id session) :refresh]))

(defn get-title [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-title"
  (-> server
      (api :get [:session (session-id session) :title])
      :value))

(defn get-window-handle [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (-> server
      (api :get [:session (session-id session) :window])
      :value))

(defn close-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (api server :delete [:session (session-id session) :window]))

(defn switch-to-window [server session handle]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-window"
  (api server :post
       [:session (session-id session) :window]
       {:handle handle}))

(defn get-window-handles [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handles"
  (-> server
      (api :get [:session (session-id session) :window :handles])
      :value))

;; Switch To Frame
;; https://www.w3.org/TR/webdriver/#dfn-switch-to-frame

;; Switch To Parent Frame
;; https://www.w3.org/TR/webdriver/#dfn-switch-to-parent-frame

(defn get-window-size [server session]
  (-> server
      (api :get [:session (session-id session) :window :size])))

(defn set-window-size [server session width height]
  (-> server
      (api :post [:session (session-id session) :window :size]
           {:width width :height height})))

(defn set-window-position [server session x y]
  (-> server
      (api :post [:session (session-id session) :window :position]
           {:x x :y y})))

(defn ^:not-implemented
  get-window-rect [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-rect"
  (-> server
      (api :get [:session (session-id session) :window :rect])))

(defn ^:not-implemented
  set-window-rect [server session x y width height]
  "https://www.w3.org/TR/webdriver/#dfn-set-window-rect"
  (api :post
       [:session (session-id session) :window :rect]
       {:x x :y y :width width :height height}))

(defn maximize-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-maximize-window"
  (api server :post
       [:session (session-id session) :window :maximize]))

(defn ^:not-implemented
  fullscreen-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-fullscreen-window"
  (api server :post
       [:session (session-id session) :window :fullscreen]))

;; todo ff only
(defn get-active-element [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-active-element"
  (-> (api server :get
           [:session (session-id session) :element :active])
      :value
      parse-element))

(defn find-element [server session locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-element"
  (-> server
      (api :post
           [:session (session-id session) :element]
           {:using locator :value term})
      :value
      parse-element))

(defn find-elements [server session locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-elements"
  (-> server
      (api :post
           [:session (session-id session) :elements]
           {:using locator :value term})
      :value
      (->> (mapv parse-element))))

(defn find-element-from-element [server session element locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-element-from-element"
  (-> server
      (api :post
           [:session (session-id session) :element element :element]
           {:using locator :value term})
      :value
      parse-element))

(defn find-elements-from-element [server session element locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-elements-from-element"
  (-> server
      (api :post
           [:session (session-id session) :element element :elements]
           {:using locator :value term})
      :value
      (->> (mapv parse-element))))

(defn is-element-displayed [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  (-> server
      (api :get
           [:session (session-id session) :element element :displayed])
      :value))

(defn is-element-selected [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  (-> server
      (api :get
           [:session (session-id session) :element element :selected])
      :value))

(defn get-element-attribute [server session element attribute]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-attribute"
  (-> server
      (api :get
           [:session (session-id session) :element element :attribute attribute])
      :value))

(defn get-element-property [server session element property]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-property"
  (-> server
      (api :get
           [:session (session-id session) :element element :property property])
      :value))

(defn get-element-css-value [server session element property]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-css-value"
  (-> server
      (api :get
           [:session (session-id session) :element element :css property])
      :value))

(defn get-element-text [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-text"
  (-> server
      (api :get
           [:session (session-id session) :element element :text])
      :value))

(defn get-element-tag-name [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-tag-name"
  (-> server
      (api :get
           [:session (session-id session) :element element :name])
      :value))

(defn get-element-rect [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-rect"
  (-> server
      (api :get
           [:session (session-id session) :element element :rect])))

(defn is-element-enabled [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (api :get
           [:session (session-id session) :element element :enabled])
      :value))

(defn element-click [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (api :post
           [:session (session-id session) :element element :click])))

(defn element-tap [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (api :post
           [:session (session-id session) :element element :tap])))

(defn element-clear [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-element-clear"
  (-> server
      (api :post
           [:session (session-id session) :element element :clear])))

(defn element-send-keys [server session element text]
  "https://www.w3.org/TR/webdriver/#dfn-element-send-keys"
  (-> server
      (api :post
           [:session (session-id session) :element element :value]
           {:value (text-to-array text)})))

(defn get-page-source [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-page-source"
  (-> server
      (api :get
           [:session (session-id session) :source])
      :value))

(defn execute-script [server session script & args]
  "https://www.w3.org/TR/webdriver/#dfn-execute-script"
  (-> server
      (api :post
           [:session (session-id session) :execute :sync]
           {:script script :args args})
      :value))

;; todo didn't catch how it works
(defn execute-async-script [server session script & args]
  "https://www.w3.org/TR/webdriver/#dfn-execute-async-script"
  (-> server
      (api :post
           [:session (session-id session) :execute :async]
           {:script script :args args})))

(defn get-all-cookies [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-all-cookies"
  (-> server
      (api :get
           [:session (session-id session) :cookie])
      :value))

(defn get-named-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-get-named-cookie"
  (-> server
      (api :get
           [:session (session-id session) :cookie name])
      :value
      first))

;; todo params
(defn add-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-add-cookie"
  (-> server
      ;; (api :post
      ;;      [:session (session-id session) :cookie]
      ;;      {:name name :value value :path path
      ;;       :domain domain :secure secure
      ;;       :httpOnly httpOnly :expiry expiry})
      ))

(defn delete-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-delete-cookie"
  (-> server
      (api :delete
           [:session (session-id session) :cookie name])))

(defn delete-all-cookies [server session]
  "https://www.w3.org/TR/webdriver/#dfn-delete-all-cookies"
  (-> server
      (api :delete
           [:session (session-id session) :cookie])))

;; todo doesn't work
(defn perform-actions [server session]
  "https://www.w3.org/TR/webdriver/#dfn-perform-implementation-specific-action-dispatch-steps"
  (-> server
      (api :post
           [:session (session-id session) :actions] ;; :data ;; id
           {:actions [{:type "pointer" :actions [{:type "pointerDown" :button 123}]}]})))

;; todo doesn't work
(defn release-actions [server session]
  "https://www.w3.org/TR/webdriver/#dfn-release-actions"
  (-> server
      (api :delete
           [:session (session-id session) :actions])))

(defn dismiss-alert [server session]
  "https://www.w3.org/TR/webdriver/#dfn-dismiss-alert"
  (-> server
      (api :post
           [:session (session-id session) :alert :dismiss])))

(defn accept-alert [server session]
  "https://www.w3.org/TR/webdriver/#dfn-accept-alert"
  (-> server
      (api :post
           [:session (session-id session) :alert :accept])))

(defn get-alert-text [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-alert-text"
  (-> server
      (api :get
           [:session (session-id session) :alert :text])
      :value))

(defn send-alert-text [server session text]
  "https://www.w3.org/TR/webdriver/#dfn-send-alert-text"
  (-> server
      (api :post
           [:session (session-id session) :alert :text]
           {:value (text-to-array text)})))

(defn take-screenshot [server session filename]
  "https://www.w3.org/TR/webdriver/#dfn-take-screenshot"
  (-> server
      (api :get
           [:session (session-id session) :screenshot])
      :value ;; todo might be empty string
      (b64-to-file filename)))

(defn take-element-screenshot [server session element filename]
  "https://www.w3.org/TR/webdriver/#dfn-take-element-screenshot"
  (-> server
      (api :get
           [:session (session-id session) :element element :screenshot])
      :value ;; todo might be empty string
      (b64-to-file filename)))
