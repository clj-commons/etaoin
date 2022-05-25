(ns ^:no-doc etaoin.impl.driver
  "Some utilities to work with driver's data structure.

  Links for development:

  Firefox command line flags:
  /Applications/Firefox.app/Contents/MacOS/firefox-bin --help

  Chrome binary path:
  /Applications/Google Chrome.app/Contents/MacOS/Google Chrome

  Chrome CLI args:
  https://peter.sh/experiments/chromium-command-line-switches/

  Chrome capabilities:
  https://sites.google.com/a/chromium.org/chromedriver/capabilities

  Firefox capabilities:
  https://github.com/mozilla/geckodriver/#firefox-capabilities

  Firefox profiles:
  https://support.mozilla.org/en-US/kb/profiles-where-firefox-stores-user-data

  Safari endpoints
  https://developer.apple.com/library/content/documentation/NetworkingInternetWeb/Conceptual/WebDriverEndpointDoc/Commands/Commands.html

  Edge capabilities and endpoints
  https://docs.microsoft.com/en-us/microsoft-edge/webdriver

  JSON Wire protocol (obsolete)
  https://github.com/SeleniumHQ/selenium/wiki/JsonWireProtocol

  Selenium Python source code for Firefox
  https://github.com/SeleniumHQ/selenium/blob/master/py/selenium/webdriver/firefox/options.py
  "
  (:require [etaoin.util :refer [defmethods deep-merge]]
            [babashka.fs :as fs]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn dispatch-driver
  [driver & _]
  (:type driver))

(defn append-args
  [args extra]
  (concat args extra))

(defn prepend-args
  [args extra]
  (concat extra args))

(defn set-path
  "Sets path to the driver's binary file."
  [driver path]
  (update driver :args prepend-args [path]))

(defn set-args
  [driver args]
  (update driver :args append-args args))

(defn get-args
  [driver]
  (or (:args driver) []))


;;
;; Port
;;

(defmulti set-port
  "Updates driver's map with the given port added to the args."
  {:arglists '([driver port])}
  dispatch-driver)

(defmethods set-port
  [:firefox :safari]
  [driver port]
  (set-args driver ["--port" port]))

(defmethods set-port
  [:chrome :edge]
  [driver port]
  (set-args driver [(str "--port=" port)]))

(defmethod set-port
  :phantom
  [driver port]
  (set-args driver ["--webdriver" port]))

;;
;; capabilities
;;

(defn set-capabilities
  [driver caps]
  (update driver :capabilities deep-merge caps))


(defn set-load-strategy
  [driver strategy]
  (assoc-in driver [:capabilities :pageLoadStrategy] strategy))


;;
;; options utils
;;

(defmulti options-name dispatch-driver) ;; todo nil default

(defmethod options-name
  :firefox
  [_driver]
  :moz:firefoxOptions)

(defmethod options-name
  :chrome
  [_driver]
  :chromeOptions)

(defmethod options-name
  :safari
  [_driver]
  :safariOptions)

(defmethod options-name
  :edge
  [_driver]
  :edgeOptions)

(defmethod options-name
  :opera
  [_driver]
  :operaOptions)

(defn set-options-args
  "Adds command line arguments for a browser binary (not a driver)."
  [driver args]
  (update-in driver
             [:capabilities (options-name driver) :args]
             append-args (map str args)))

;;
;; Profiles
;;

(defmulti set-profile dispatch-driver)

(defmethod set-profile
  :default
  [driver _profile]
  (log/infof "This driver doesn't support setting a profile.")
  driver)

(defmethod set-profile
  :chrome
  ;; Chrome adds the trailing `/Default` part to the profile path.
  ;; To prevent duplication, let's clear the given path manually.
  [driver ^String profile]
  (let [profile       (fs/file profile)
        profile (if (= "Default" (fs/file-name profile))
                  (fs/parent profile)
                  profile)
        user-data-dir (str (fs/parent profile))
        profile-dir   (str  (fs/file-name profile))]
    (set-options-args driver [(format "--user-data-dir=%s" user-data-dir)
                              (format "--profile-directory=%s" profile-dir)])))

(defmethod set-profile
  :firefox
  ;; When setting a custom profile, geckodriver cannot
  ;; connect to the browser. The issue
  ;; https://github.com/mozilla/geckodriver/issues/1058
  ;; says to specify a marionette port manually.
  [driver profile]
  (-> driver
      (set-options-args ["-profile" profile])
      ((fn [driver]
         (if (some #(= "--marionette-port" %) (get-args driver))
           driver
           (set-args driver ["--marionette-port" 2828]))))))

;;
;; window size
;;

(defmulti set-window-size
  "Adds browser's command line arguments for setting initial window size."
  {:arglists '([driver w h])}
  dispatch-driver)

(defmethod set-window-size
  :default
  [driver _w _h]
  (log/infof "This driver doesn't support setting window size.")
  driver)

(defmethod set-window-size
  :chrome
  [driver w h]
  (set-options-args driver [(format "--window-size=%s,%s" w h)]))

(defmethod set-window-size
  :firefox
  [driver w h]
  (set-options-args driver ["-width" w "-height" h]))

;;
;; initial URL
;;

(defmulti set-url
  "Sets the default URL that the browser should open by default."
  {:arglists '([driver url])}
  dispatch-driver)

(defmethod set-url
  :default
  [driver _url]
  (log/infof "This driver doesn't support setting initial URL.")
  driver)

(defmethod set-url
  :firefox
  [driver url]
  (set-options-args driver ["--new-window" url]))

;; Don't know why but Chrome ignores all the --new-window, --app
;; or --google-base-url parameters when starting.

;;
;; headless feature
;;

(defmulti set-headless
  {:arglists '([driver])}
  dispatch-driver)

(defmethod set-headless
  :default
  [driver]
  (log/infof "This driver doesn't support setting headless mode.")
  driver)

(defmethods set-headless
  [:edge :chrome :firefox]
  [driver]
  (-> driver
      (assoc :headless true)
      (set-options-args ["--headless"])))

(defmulti is-headless?
  {:arglists '([driver])}
  dispatch-driver)

(defmethod is-headless?
  :default
  [driver]
  (if-let [args (get-in driver [:capabilities (options-name driver) :args])]
    (contains? (set args) "--headless")
    (:headless driver)))

(defmethod is-headless?
  :phantom
  [_driver]
  true)

;;
;; HTTP proxy
;;

(defn proxy->w3c
  [proxy]
  (let [{:keys [http ssl ftp socks pac-url bypass]} proxy]
    (cond-> nil
      (or ssl http
          ftp socks) (assoc :proxyType "manual")
      pac-url        (assoc :proxyType "pac"
                            :proxyAutoconfigUrl pac-url)
      http           (assoc :httpProxy http)
      ssl            (assoc :sslProxy ssl)
      ftp            (assoc :ftpProxy ftp)
      socks          (assoc :socksProxy (:host socks)
                            :socksVersion (or (:version socks) 5))
      bypass         (assoc :noProxy bypass))))

(defn set-proxy
  [driver proxy]
  (let [proxy-w3c (proxy->w3c proxy)]
    (set-capabilities driver {:proxy proxy-w3c})))

;;
;; Custom preferences
;;

(defmulti set-prefs
  {:arglists '([driver prefs])}
  dispatch-driver)

(defmethod set-prefs
  :default
  [driver _prefs]
  (log/infof "This driver doesn't support setting preferences.")
  driver)

(defmethods set-prefs
  [:firefox :chrome]
  [driver prefs]
  (update-in driver
             [:capabilities (options-name driver) :prefs]
             merge prefs))

;;
;; Download folder
;;

(defn- add-trailing-slash
  ^String [^String path]
  (let [sep java.io.File/separator]
    (if (string/ends-with? path sep)
      path
      (str path sep))))

(defmulti set-download-dir
  {:arglists '([driver path])}
  dispatch-driver)

(defmethod set-download-dir
  :default
  [driver _path]
  (log/infof "This driver doesn't support setting a download directory.")
  driver)

;; https://github.com/rshf/chromedriver/issues/338
;; trailing slash is mandatory for Chrome
(defmethod set-download-dir
  :chrome
  [driver path]
  (set-prefs driver {:download.default_directory   (add-trailing-slash path)
                     :download.prompt_for_download false}))

(def ^{:private true
       :doc     "A set of content types that should be downloaded without asking a user."}
  ff-content-types
  #{"application/gzip"
    "application/json"
    "application/msword"
    "application/octet-stream"
    "application/pdf"
    "application/rtf"
    "application/vnd.ms-excel"
    "application/vnd.ms-powerpoint"
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "application/x-7z-compressed"
    "application/x-rar-compressed"
    "application/x-shockwave-flash"
    "application/x-tar"
    "application/zip"
    "audio/flac"
    "audio/mpeg"
    "audio/ogg"
    "image/svg+xml"
    "text/csv"
    "text/javascript"
    "text/plain"
    "text/xml"
    "video/mp4"
    "video/mpeg"
    "video/ogg"
    "video/quicktime"
    "video/webm"
    "video/x-flv"
    "video/x-msvideo"})

(defmethod set-download-dir
  :firefox
  [driver path]
  (set-prefs driver {:browser.download.dir            path
                     :browser.download.folderList     2
                     :browser.download.useDownloadDir true
                     :browser.helperApps.neverAsk.saveToDisk
                     (string/join ";" ff-content-types)}))

;;
;; binary path
;;

(defmulti set-binary
  {:arglists '([driver binary])}
  dispatch-driver)

(defmethod set-binary
  :default
  [driver binary]
  (assoc-in driver
            [:capabilities (options-name driver) :binary]
            binary))

;;
;; logging
;;

(defn- remap-log-level
  "Mapping from a human-friendly log level to a system one."
  [level]
  (case level
    (nil
      :off
      :none)    "OFF"
    :debug      "DEBUG"
    :info       "INFO"
    (:warn
     :warning)  "WARNING"
    (:err
     :error
     :severe
     :crit
     :critical) "SEVERE"
    :all        "ALL"
    (assert false (format "Logging level %s is unsupported." level))))


;;
;; https://github.com/SeleniumHQ/selenium/wiki/DesiredCapabilities#loggingpreferences-json-object
;;
(defmulti set-browser-log-level
  "Sets browser logging level."
  {:arglists '([driver binary])}
  dispatch-driver)

(defmethod set-browser-log-level
  :default
  [driver level]
  (assoc-in driver
            [:capabilities :loggingPrefs :browser]
            (remap-log-level level)))


;; http://chromedriver.chromium.org/capabilities
;; http://chromedriver.chromium.org/logging/performance-log
(defn set-perf-logging
  "
  categories example:
  [:browser :devtools :devtools.timeline]
  "
  [driver & [{:keys [level network? page? categories interval]
              :or   {level      :all
                     network?   true
                     page?      false
                     categories [:devtools.network]
                     interval   1000}}]]
  (update driver :capabilities
          (fn [capabilities]
            (-> capabilities
                (assoc-in [:loggingPrefs :performance]
                          (remap-log-level level))
                (assoc-in [(options-name driver) :perfLoggingPrefs]
                          {:enableNetwork                network?
                           :enablePage                   page?
                           :traceCategories              (string/join "," (map name categories))
                           :bufferUsageReportingInterval interval})))))

(defmulti set-driver-log-level
  dispatch-driver)

(defmethod set-driver-log-level
  :default
  [driver _]
  (log/infof "The log level setting is not implemented for this driver.")
  driver)

(defmethod set-driver-log-level
  :chrome
  [driver log-level]
  (set-args driver [(format "--log-level=%s" log-level)]))

(defmethod set-driver-log-level
  :firefox
  [driver log-level]
  (set-args driver ["--log" log-level]))

(defmethod set-driver-log-level
  :phantom
  [driver log-level]
  (set-args driver [(format "--webdriver-loglevel=%s" log-level)]))


;;
;; User-Agent
;; https://stackoverflow.com/questions/29916054/
;;

(defmulti set-user-agent
  "Set User-Agent header for the driver."
  {:arglists '([driver user-agent])}
  dispatch-driver)

(defmethods set-user-agent
  [:chrome :edge]
  [driver user-agent]
  (set-options-args driver [(str "--user-agent=" user-agent)]))

(defmethods set-user-agent
  [:firefox]
  [driver user-agent]
  (set-prefs driver {:general.useragent.override user-agent}))

(defmethods set-user-agent
  [:default]
  [driver _user-agent]
  (log/infof "This driver doesn't support setting a user-agent.")
  driver)
