(ns etaoin.driver
  "Some utilities to work with driver's data structure.

  Note: the functions below take not an atom but pure map
  to be used with swap!. Our further goal is to reduce atom usage
  everywhere it is possible.

  Firefox command line flags:
  /Applications/Firefox.app/Contents/MacOS/firefox-bin --help

  Chrome binary path:
  /Applications/Google Chrome.app/Contents/MacOS/Google Chrome

"
  (:require [etaoin.util :refer [defmethods deep-merge]]
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

(defn set-capabilities
  [driver caps]
  (update driver :capabilities deep-merge caps))

;; https://github.com/SeleniumHQ/selenium/blob/master/py/selenium/webdriver/firefox/options.py
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
  "Adds command line arguments for the window initial size."
  [driver args]
  (update-in driver
             [:capabilities (options-name driver) :args]
             append-args (map str args)))

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
