(ns etaoin.api2
  "Alternate syntax for [[etaoin.api]] with-<driver> calls."
  (:require
   [etaoin.api :as e]))

(defmacro with-firefox
  "Executes `body` with a Firefox driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-firefox [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :firefox ~opts ~bind
     ~@body))

(defmacro with-chrome
  "Executes `body` with a Chrome driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-chrome [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :chrome ~opts ~bind
     ~@body))

(defmacro with-edge
  "Executes `body` with a Edge driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-edge [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :edge ~opts ~bind
     ~@body))

(defmacro with-phantom
  "Executes `body` with a Phantom.JS driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-phantom [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :phantom ~opts ~bind
     ~@body))

(defmacro with-safari
  "Executes `body` with a Safari driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-safari [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :safari ~opts ~bind
     ~@body))

(defmacro with-chrome-headless
  "Executes `body` with a headless Chrome driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-chrome-headless [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :chrome (assoc ~opts :headless true) ~bind
     ~@body))


(defmacro with-firefox-headless
  "Executes `body` with a headless Firefox driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `opts` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-firefox-headless [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :firefox (assoc ~opts :headless true) ~bind
     ~@body))


(defmacro with-edge-headless
  "Executes `body` with a headless Edge driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-edge-headless [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [opts]] & body]
  `(e/with-driver :edge (assoc ~opts :headless true) ~bind
     ~@body))
