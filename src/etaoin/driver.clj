(ns etaoin.driver
  "Some utilities to work with driver's data structure.

  Note: the functions below take not an atom but pure map
  to be used with swap!. Our further goal is to reduce atom usage
  everywhere it is possible.

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

  JSON Wire protocol (obsolete)
  https://github.com/SeleniumHQ/selenium/wiki/JsonWireProtocol

  Selenium Python source code for Firefox
  https://github.com/SeleniumHQ/selenium/blob/master/py/selenium/webdriver/firefox/options.py
"
  (:require [etaoin.util :refer [defmethods deep-merge]]
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
;; port
;;

(defmulti set-port
  "Updates driver's map with the given port added to the args."
  {:arglists '([driver port])}
  dispatch-driver)

(defmethods set-port
  [:firefox :safari]
  [driver port]
  (set-args driver ["--port" port]))

(defmethod set-port
  :chrome
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

;;
;; options utils
;;

(defmulti options-name dispatch-driver)

(defmethod options-name
  :firefox
  [driver]
  :moz:firefoxOptions)

(defmethod options-name
  :chrome
  [driver]
  :chromeOptions)

(defmethod options-name
  :safari
  [driver]
  :safariOptions) ;; todo check

(defmethod options-name
  :opera
  [driver]
  :operaOptions)

(defn set-options-args
  "Adds command line arguments for a browser binary (not a driver)."
  [driver args]
  (update-in driver
             [:capabilities (options-name driver) :args]
             append-args (map str args)))

;;
;; profiles
;;

(defmulti set-profile dispatch-driver)

(defmethod set-profile
  :default
  [driver profile]
  (log/debugf "This driver doesn't support setting a profile.")
  driver)

(defmethod set-profile
  :chrome
  ;; Chrome adds the trailing `/Default` part to the profile path.
  ;; To prevent duplication, let's clear the given path manually.
  [driver profile]
  (let [default #"(\\|/)Default$"
        p (string/replace profile default "")]
    (set-options-args driver [(format "--user-data-dir=%s" p)])))

(defmethod set-profile
  :firefox
  ;; When setting a custom profile, geckodriver cannot
  ;; connect to the browser. The issue
  ;; https://github.com/mozilla/geckodriver/issues/1058
  ;; says to specify a marionette port manually.
  [driver profile]
  (-> driver
      (set-options-args ["-profile" profile])
      (set-args ["--marionette-port" 2828])))

;;
;; window size
;;

(defmulti set-window-size
  "Adds browser's command line arguments for setting initial window size."
  {:arglists '([driver w h])}
  dispatch-driver)

(defmethod set-window-size
  :default
  [driver w h]
  (log/debugf "This driver doesn't support setting window size.")
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
  [driver url]
  (log/debugf "This driver doesn't support setting initial URL.")
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
  (log/debugf "This driver doesn't support setting headless mode.")
  driver)

(defmethods set-headless
  [:chrome :firefox]
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
  (:headless driver))

(defmethod is-headless?
  :phantom
  [driver]
  true)

;;
;; Custom preferences
;;

(defmulti set-prefs
  {:arglists '([driver prefs])}
  dispatch-driver)

(defmethod set-prefs
  :default
  [driver prefs]
  (log/debugf "This driver doesn't support setting preferences.")
  driver)

(defmethods set-prefs
  [:firefox :chrome]
  [driver prefs]
  (update-in driver
             [:capabilities (options-name driver) :prefs]
             merge prefs))

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
     :none)     "OFF"
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
