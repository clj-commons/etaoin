(ns etaoin.api2
  "Improved syntax for some [[etaoin.api]] calls"
  (:require
   [etaoin.api :as e]))

(defmacro with-firefox
  "Executes `body` with a Firefox driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-firefox [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :firefox ~options ~bind
     ~@body))

(defmacro with-chrome
  "Executes `body` with a Chrome driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-chrome [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :chrome ~options ~bind
     ~@body))

(defmacro with-edge
  "Executes `body` with a Edge driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-edge [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :edge ~options ~bind
     ~@body))


(defmacro with-phantom
  "Executes `body` with a Phantom.JS driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-phantom [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :phantom ~options ~bind
     ~@body))


(defmacro with-safari
  "Executes `body` with a Safari driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-safari [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :safari ~options ~bind
     ~@body))


(defmacro with-chrome-headless
  "Executes `body` with a headless Chrome driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-chrome-headless [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :chrome (assoc ~options :headless true) ~bind
     ~@body))


(defmacro with-firefox-headless
  "Executes `body` with a headless Firefox driver session bound to `bind`.

  Driver is automatically launched and terminated (even if an exception occurs).

  `options` - optional, see [Driver Options](/doc/01-user-guide.adoc#driver-options).

  Example:

  ```Clojure
  (with-firefox-headless [driver]
    (go driver \"https://clojure.org\"))
  ```"
  [[bind & [options]] & body]
  `(e/with-driver :firefox (assoc ~options :headless true) ~bind
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
  [[bind & [options]] & body]
  `(e/with-driver :edge (assoc ~options :headless true) ~bind
     ~@body))
