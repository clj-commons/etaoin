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
  https://github.com/SeleniumHQ/selenium/blob/master/py/selenium/webdriver/firefox/options.py"
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [etaoin.impl.util :refer [deep-merge defmethods]]))

(set! *warn-on-reflection* true)

(defn- osify-path [s]
  (-> s
      fs/file
      str))

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

(defn- unsupported-msg [driver feature]
  (format "%s does not support %s" (:type driver) feature))

;;
;; WebDriver Port - via webdriver command line arg
;;

(defmulti set-port
  "Communication port for the webdriver, set as a command line arg."
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

;;
;; Capabilities - are sent during webdriver session creation
;;

(defn set-capabilities
  [driver caps]
  (if caps
    (update driver :capabilities deep-merge caps)
    driver))

;;
;; Load strategy is a w3c webdrive spec capability
;;

(defn set-load-strategy
  "Page load strategy is part of the w3c spec"
  [driver strategy]
  (assoc-in driver [:capabilities :pageLoadStrategy] strategy))

;;
;; Vendor specific options are specified in capabalities under a vendor specific name
;;

(defmulti vendor-options-name dispatch-driver) ;; todo nil default

(defmethod vendor-options-name
  :firefox
  [_driver]
  :moz:firefoxOptions)

(defmethod vendor-options-name
  :chrome
  [_driver]
  :goog:chromeOptions)

(defmethod vendor-options-name
  :safari
  [_driver]
  :safari:options)

(defmethod vendor-options-name
  :edge
  [_driver]
  :ms:edgeOptions)

(defn- update-vendor-capabilities [driver key f val]
  (update-in driver
             [:capabilities (vendor-options-name driver) key]
             f val))

(defn- set-vendor-capabilities [driver key val]
  (assoc-in driver
            [:capabilities (vendor-options-name driver) key]
            val))

(defn add-browser-args
  "Adds command line arguments for the browser binary (not the webdriver binary)."
  [driver args]
  (update-vendor-capabilities driver :args append-args (map str args)))

;;
;; Profiles - if supported, are set via browser args
;;

;; vendor specific
(defmulti set-profile dispatch-driver)

(defmethod set-profile
  :default
  [driver _profile]
  (log/infof (unsupported-msg driver "setting a profile"))
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
    (add-browser-args driver [(format "--user-data-dir=%s" (osify-path user-data-dir))
                              (format "--profile-directory=%s" (osify-path profile-dir))])))

(defmethod set-profile
  :firefox
  ;; When setting a custom profile, geckodriver cannot
  ;; connect to the browser. The issue
  ;; https://github.com/mozilla/geckodriver/issues/1058
  ;; says to specify a marionette port manually.
  [driver profile]
  (-> driver
      (add-browser-args ["-profile" (osify-path profile)])
      ((fn [driver]
         (if (some #(= "--marionette-port" %) (get-args driver))
           driver
           (set-args driver ["--marionette-port" 2828]))))))

;;
;; Browser initial window size, if supported, is set via command line args
;;

(defmulti set-window-size
  "Adds browser's command line arguments for setting initial window size."
  {:arglists '([driver w h])}
  dispatch-driver)

(defmethod set-window-size
  :default
  [driver _w _h]
  (log/infof (unsupported-msg driver "setting initial window size"))
  driver)

(defmethods set-window-size
  [:chrome :edge]
  [driver w h]
  (add-browser-args driver [(format "--window-size=%s,%s" w h)]))

(defmethod set-window-size
  :firefox
  [driver w h]
  (add-browser-args driver ["-width" w "-height" h]))

;;
;; Initial URL - Set, if supported, via browser arg
;;

(defmulti set-url
  "Sets the default URL that the browser should open by default."
  {:arglists '([driver url])}
  dispatch-driver)

(defmethod set-url
  :default
  [driver _url]
  (log/infof (unsupported-msg driver "setting initial URL to load"))
  driver)

(defmethod set-url
  :firefox
  [driver url]
  (add-browser-args driver ["--new-window" url]))

;; Don't know why but Chrome ignores all the --new-window, --app
;; or --google-base-url parameters when starting.

;;
;; Headless mode, if supported, is supported via browser arg
;;

(defmulti set-headless
  {:arglists '([driver])}
  dispatch-driver)

(defmethod set-headless
  :default
  [driver]
  (log/infof (unsupported-msg driver "headless mode"))
  driver)

(defmethods set-headless
  [:edge :chrome :firefox]
  [driver]
  (-> driver
      (assoc :headless true)
      (add-browser-args ["--headless"])))

(defn is-headless?
  [driver]
  (if-let [args (get-in driver [:capabilities (vendor-options-name driver) :args])]
    (contains? (set args) "--headless")
    (:headless driver)))

;;
;; HTTP proxy - is part of the w3c webdriver capabilities spec
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
  "The proxy is part of the w3c spec"
  [driver proxy]
  (let [proxy-w3c (proxy->w3c proxy)]
    (set-capabilities driver {:proxy proxy-w3c})))

;;
;; Custom preferences - are vendor specific capabilities
;;

(defmulti set-prefs
  {:arglists '([driver prefs])}
  dispatch-driver)

(defmethod set-prefs
  :default
  [driver _prefs]
  (log/info (unsupported-msg driver "setting vendor preferences"))
  driver)

(defmethods set-prefs
  [:firefox :chrome :edge]
  [driver prefs]
  (update-vendor-capabilities driver :prefs merge prefs))

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
  (log/info (unsupported-msg driver "setting download directory"))
  driver)

;; https://github.com/rshf/chromedriver/issues/338
;; trailing slash is mandatory for Chrome
(defmethods set-download-dir
  [:chrome :edge]
  [driver path]
  (set-prefs driver {:download.default_directory   (-> path osify-path add-trailing-slash)
                     :download.prompt_for_download false}))

(def ^{:private true
       :doc     "A set of content types that should be downloaded without asking a user."}
  firefox-content-types
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
                     (string/join ";" firefox-content-types)}))

;;
;; Browser binary path - is set under vendor specific capability
;;

(defn set-browser-binary
  [driver binary]
  (set-vendor-capabilities driver :binary binary))

;;
;; Browser console logging - is set via vendor specific settings
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
    (assert false (format "Logging level %s is unrecognized." level))))

(comment
  (remap-log-level :off)
  (remap-log-level :foo)

  )

;;
;; These used to be supported by loggingPrefs, but with newer capabilities model
;; are browser specific.
;;
(defmulti set-browser-log-level
  "Sets browser logging level."
  {:arglists '([driver binary])}
  dispatch-driver)

(defmethod set-browser-log-level
  :default
  [driver _level]
  (log/info (unsupported-msg driver "setting the browser log level"))
  driver)

(defmethods set-browser-log-level
  [:chrome :edge]
  [driver level]
  (assoc-in driver
            ;; supports: ALL, DEBUG, INFO, WARNING, SEVERE, or OFF.
            ;; a bit counter-intuitive, but does not go under goog:chromeOptions
            [:capabilities :goog:loggingPrefs :browser]
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
                (assoc-in [:goog:loggingPrefs :performance]
                          (remap-log-level level))
                (assoc-in [(vendor-options-name driver) :perfLoggingPrefs]
                          {:enableNetwork                network?
                           :enablePage                   page?
                           :traceCategories              (string/join "," (map name categories))
                           :bufferUsageReportingInterval interval})))))

(defmulti set-driver-log-level
  dispatch-driver)

(defmethods set-driver-log-level
  [:chrome :edge]
  [driver log-level]
  (set-args driver [(format "--log-level=%s" log-level)]))

(defmethod set-driver-log-level
  :firefox
  [driver log-level]
  (set-args driver ["--log" log-level]))

(defmethod set-driver-log-level
  :safari
  [driver log-level]
  (when-not (= "debug" (string/lower-case log-level))
    (throw (ex-info "Safari Driver only supports debug level logging" {})))
  (-> (set-args driver ["--diagnose"])
      (update :post-run-actions (fnil conj []) :discover-safari-webdriver-log)))

;;
;; User-Agent - supported through various custom schemes
;; https://stackoverflow.com/questions/29916054/
;;

(defmulti set-user-agent
  "Set User-Agent header for the driver."
  {:arglists '([driver user-agent])}
  dispatch-driver)

(defmethods set-user-agent
  [:default]
  [driver _user-agent]
  (log/info (unsupported-msg driver "setting the user-agent" ))
  driver)

(defmethods set-user-agent
  [:chrome :edge]
  [driver user-agent]
  (add-browser-args driver [(str "--user-agent=" user-agent)]))

(defmethods set-user-agent
  [:firefox]
  [driver user-agent]
  (set-prefs driver {:general.useragent.override user-agent}))
