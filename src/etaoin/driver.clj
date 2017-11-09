(ns etaoin.driver
  "Some utilities to work with driver's data structure.

  Note: the functions below take not an atom but pure map
  to be used with swap!. Our further goal is to reduce atom usage
  everywhere it is possible.

  Firefox command line flags:
  /Applications/Firefox.app/Contents/MacOS/firefox-bin --help

"
  (:require [etaoin.util :refer [defmethods deep-merge]]
            [clojure.tools.logging :as log]))

(defn dispatch-driver
  [driver & _]
  (:type driver))

(defn prepend
  "Puts an element into a seq's head returning a new lazy seq."
  [seq x]
  (cons x seq))

(defn set-args
  "Sets browser's command line arguments."
  [driver args]
  (update driver :args concat args))

(defn set-path
  "Sets path to the driver's binary file."
  [driver path]
  (update driver :args prepend path))

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

(defmethods set-port
  [:chrome :headless]
  [driver port]
  (set-args driver [(str "--port=" port)]))

(defmethod set-port
  :phantom
  [driver port]
  (set-args driver ["--webdriver" port]))

(defn set-capabilities
  [driver caps]
  (update driver :capabilities deep-merge caps))

(defmulti set-options-args
  "Adds command line arguments for options object
  (chromeOptions, FirefoxOptions, etc)."
  {:arglists '([driver args])}
  dispatch-driver)

(defmethod set-options-args
  :default
  [driver args]
  (log/debugf "Your browser doesn't support setting options.")
  driver)

(defmethods set-options-args
  [:chrome :headless]
  [driver args]
  (update-in driver
             [:capabilities :chromeOptions :args]
             concat args))

(defmethod set-options-args
  :firefox
  [driver args]
  (update-in driver
             [:capabilities :FirefoxOptions :args]
             concat args))

;; see https://github.com/SeleniumHQ/selenium/blob/master/py/selenium/webdriver/firefox/options.py
(defmulti options-name dispatch-driver)

(defmethod options-name
  :firefox
  [driver]
  :moz:firefoxOptions)

(defmethods options-name
  [:chrome :headless]
  [driver]
  :chromeOptions)

(defn set-options-args
  "Adds command line arguments for the window initial size."
  [driver args]
  (update-in driver
             [:capabilities (options-name driver) :args]
             concat (map str args)))

(defmulti set-window-size
  "Adds browser's command line arguments for setting initial window size."
  {:arglists '([driver w h])}
  dispatch-driver)

;; todo unsure about it
#_(defmethod set-window-size
  :default
  [driver w h]
  (log/debugf "Your browser doesn't support window size arguments")
  driver)

(defmethods set-window-size
  [:chrome :headless]
  [driver w h]
  (set-options-args driver [(format "--window-size=%s,%s" w h)]))

(defmethod set-window-size
  :firefox
  [driver w h]
  (set-options-args driver ["-width" w "-height" h]))

;; todo add initial url
;; (set-options-args driver ["--new-tab" "http://ya.ru"])
