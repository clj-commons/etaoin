(ns webdriver.api
  (:require [webdriver.client :as client]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            ))

;;
;; todos
;; default capabilities
;; locator keyword

;;
;; params
;;


(def default-capabilities
  {:browserName "firefox"
   :javascriptEnabled true
   :acceptSslCerts true
   :platform "ANY"
   :marionette true
   :name "Sample Test"})

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
  (-> data first second))

(defn b64-to-file [b64str filename]
  (with-open [out (io/output-stream filename)]
    (.write out (-> b64str
                    .getBytes
                    b64/decode))))

;;
;; client/call
;;

(defn new-session [server capabilities]
  "https://www.w3.org/TR/webdriver/#dfn-new-session"
  (client/call server :post [:session]
               {:desiredCapabilities (merge default-capabilities
                                            capabilities)}))

(defn delete-session [server session]
  "https://www.w3.org/TR/webdriver/#dfn-delete-session"
  (client/call server
               :delete
               [:session (session-id session)]))

(defn status [server]
  "https://www.w3.org/TR/webdriver/#dfn-status"
  (client/call server :get [:status]))

(defn get-timeout [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-timeout"
  (client/call server :get
               [:session (session-id session) :timeouts]))

(defn set-timeout [server session type msec]
  "https://www.w3.org/TR/webdriver/#dfn-set-timeouts"
  (client/call server :post
               [:session (session-id session) :timeouts]
               {:type type :ms msec}))

(defn go [server session url]
  "https://www.w3.org/TR/webdriver/#dfn-go"
  (client/call server
               :post
               [:session (session-id session) :url]
               {:url url}))

(defn get-current-url [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-current-url"
  (client/call server
               :get
               [:session (session-id session) :url]))

(defn back [server session]
  "https://www.w3.org/TR/webdriver/#dfn-back"
  (client/call server
               :post
               [:session (session-id session) :back]))

(defn forward [server session]
  "https://www.w3.org/TR/webdriver/#dfn-forward"
  (client/call server
               :post
               [:session (session-id session) :forward]))

(defn refresh [server session]
  "https://www.w3.org/TR/webdriver/#dfn-refresh"
  (client/call server
               :post
               [:session (session-id session) :refresh]))

(defn get-title [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-title"
  (-> server
      (client/call :get [:session (session-id session) :title])
      :value))

(defn get-window-handle [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (-> server
      (client/call :get [:session (session-id session) :window])
      :value))

(defn close-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handle"
  (client/call server :delete [:session (session-id session) :window]))

(defn switch-to-window [server session handle]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-window"
  (client/call server :post
               [:session (session-id session) :window]
               {:handle handle}))

(defn get-window-handles [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-handles"
  (-> server
      (client/call :get [:session (session-id session) :window :handles])
      :value))

(defn switch-to-frame [server session frame]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-frame"
  (-> server
      (client/call
       :post [:session (session-id session) :frame]
       {:id frame})))

(defn switch-to-parent-frame [server session]
  "https://www.w3.org/TR/webdriver/#dfn-switch-to-parent-frame"
  (-> server
      (client/call
       :post [:session (session-id session) :frame :parent])))

(defn get-window-size [server session]
  (-> server
      (client/call :get [:session (session-id session) :window :size])))

(defn set-window-size [server session width height]
  (-> server
      (client/call :post [:session (session-id session) :window :size]
                   {:width width :height height})))

(defn set-window-position [server session x y]
  (-> server
      (client/call :post [:session (session-id session) :window :position]
                   {:x x :y y})))

(defn ^:not-implemented
  get-window-rect [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-window-rect"
  (-> server
      (client/call :get [:session (session-id session) :window :rect])))

(defn ^:not-implemented
  set-window-rect [server session x y width height]
  "https://www.w3.org/TR/webdriver/#dfn-set-window-rect"
  (client/call :post
               [:session (session-id session) :window :rect]
               {:x x :y y :width width :height height}))

(defn maximize-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-maximize-window"
  (client/call server :post
               [:session (session-id session) :window :maximize]))

(defn ^:not-implemented
  fullscreen-window [server session]
  "https://www.w3.org/TR/webdriver/#dfn-fullscreen-window"
  (client/call server :post
               [:session (session-id session) :window :fullscreen]))

;; ff only
(defn get-active-element [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-active-element"
  (-> (client/call server :get
                   [:session (session-id session) :element :active])
      :value
      parse-element))

(defn find-element [server session locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-element"
  (-> server
      (client/call :post
                   [:session (session-id session) :element]
                   {:using locator :value term})
      :value
      parse-element))

(defn find-elements [server session locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-elements"
  (-> server
      (client/call :post
                   [:session (session-id session) :elements]
                   {:using locator :value term})
      :value
      (->> (mapv parse-element))))

(defn find-element-from-element [server session element locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-element-from-element"
  (-> server
      (client/call :post
                   [:session (session-id session) :element element :element]
                   {:using locator :value term})
      :value
      parse-element))

(defn find-elements-from-element [server session element locator term]
  "https://www.w3.org/TR/webdriver/#dfn-find-elements-from-element"
  (-> server
      (client/call :post
                   [:session (session-id session) :element element :elements]
                   {:using locator :value term})
      :value
      (->> (mapv parse-element))))

(defn is-element-displayed [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :displayed])
      :value))

(defn is-element-selected [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-selected"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :selected])
      :value))

(defn get-element-attribute [server session element attribute]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-attribute"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :attribute attribute])
      :value))

(defn get-element-property [server session element property]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-property"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :property property])
      :value))

(defn get-element-css-value [server session element property]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-css-value"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :css property])
      :value))

(defn get-element-text [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-text"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :text])
      :value))

(defn get-element-tag-name [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-tag-name"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :name])
      :value))

(defn get-element-rect [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-get-element-rect"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :rect])))

(defn is-element-enabled [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :enabled])
      :value))

(defn element-click [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (client/call :post
                   [:session (session-id session) :element element :click])))

(defn element-tap [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-is-element-enabled"
  (-> server
      (client/call :post
                   [:session (session-id session) :element element :tap])))

(defn element-clear [server session element]
  "https://www.w3.org/TR/webdriver/#dfn-element-clear"
  (-> server
      (client/call :post
                   [:session (session-id session) :element element :clear])))

(defn element-send-keys [server session element text]
  "https://www.w3.org/TR/webdriver/#dfn-element-send-keys"
  (-> server
      (client/call :post
                   [:session (session-id session) :element element :value]
                   {:value (text-to-array text)})))

(defn get-page-source [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-page-source"
  (-> server
      (client/call :get
                   [:session (session-id session) :source])
      :value))

(defn execute-script [server session script & args]
  "https://www.w3.org/TR/webdriver/#dfn-execute-script"
  (-> server
      (client/call :post
                   [:session (session-id session) :execute :sync]
                   {:script script :args args})
      :value))

;; todo didn't catch how it works
(defn execute-async-script [server session script & args]
  "https://www.w3.org/TR/webdriver/#dfn-execute-async-script"
  (-> server
      (client/call :post
                   [:session (session-id session) :execute :async]
                   {:script script :args args})))

(defn get-all-cookies [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-all-cookies"
  (-> server
      (client/call :get
                   [:session (session-id session) :cookie])
      :value))

(defn get-named-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-get-named-cookie"
  (-> server
      (client/call :get
                   [:session (session-id session) :cookie name])
      :value
      first))

;; todo params
(defn add-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-add-cookie"
  (-> server
      ;; (client/call :post
      ;;      [:session (session-id session) :cookie]
      ;;      {:name name :value value :path path
      ;;       :domain domain :secure secure
      ;;       :httpOnly httpOnly :expiry expiry})
      ))

(defn delete-cookie [server session name]
  "https://www.w3.org/TR/webdriver/#dfn-delete-cookie"
  (-> server
      (client/call :delete
                   [:session (session-id session) :cookie name])))

(defn delete-all-cookies [server session]
  "https://www.w3.org/TR/webdriver/#dfn-delete-all-cookies"
  (-> server
      (client/call :delete
                   [:session (session-id session) :cookie])))

;; todo doesn't work
;; {:actions [{:type "pointer" :actions [{:type "pointerDown" :button 123}]}]}
(defn perform-actions [server session actions]
  "https://www.w3.org/TR/webdriver/#dfn-perform-implementation-specific-action-dispatch-steps"
  (-> server
      (client/call :post
                   [:session (session-id session) :actions])))

;; todo doesn't work
(defn release-actions [server session]
  "https://www.w3.org/TR/webdriver/#dfn-release-actions"
  (-> server
      (client/call :delete
                   [:session (session-id session) :actions])))

(defn dismiss-alert [server session]
  "https://www.w3.org/TR/webdriver/#dfn-dismiss-alert"
  (-> server
      (client/call :post
                   [:session (session-id session) :alert :dismiss])))

(defn accept-alert [server session]
  "https://www.w3.org/TR/webdriver/#dfn-accept-alert"
  (-> server
      (client/call :post
                   [:session (session-id session) :alert :accept])))

(defn get-alert-text [server session]
  "https://www.w3.org/TR/webdriver/#dfn-get-alert-text"
  (-> server
      (client/call :get
                   [:session (session-id session) :alert :text])
      :value))

(defn send-alert-text [server session text]
  "https://www.w3.org/TR/webdriver/#dfn-send-alert-text"
  (-> server
      (client/call :post
                   [:session (session-id session) :alert :text]
                   {:value (text-to-array text)})))

(defn take-screenshot [server session filename]
  "https://www.w3.org/TR/webdriver/#dfn-take-screenshot"
  (-> server
      (client/call :get
                   [:session (session-id session) :screenshot])
      :value ;; todo might be empty string
      (b64-to-file filename)))

(defn take-element-screenshot [server session element filename]
  "https://www.w3.org/TR/webdriver/#dfn-take-element-screenshot"
  (-> server
      (client/call :get
                   [:session (session-id session) :element element :screenshot])
      :value ;; todo might be empty string
      (b64-to-file filename)))
