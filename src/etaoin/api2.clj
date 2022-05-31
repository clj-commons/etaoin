(ns etaoin.api2
  "
  Better syntax for some API that cannot be fixed
  without breaking them.
  "
  (:require
   [etaoin.api :as e]))

(defmacro with-firefox
  [[bind & [options]] & body]
  `(e/with-driver :firefox ~options ~bind
     ~@body))

(defmacro with-chrome
  [[bind & [options]] & body]
  `(e/with-driver :chrome ~options ~bind
     ~@body))

(defmacro with-edge
  [[bind & [options]] & body]
  `(e/with-driver :edge ~options ~bind
     ~@body))


(defmacro with-phantom
  [[bind & [options]] & body]
  `(e/with-driver :phantom ~options ~bind
     ~@body))


(defmacro with-safari
  [[bind & [options]] & body]
  `(e/with-driver :safari ~options ~bind
     ~@body))


(defmacro with-chrome-headless
  [[bind & [options]] & body]
  `(e/with-driver :chrome (assoc ~options :headless true) ~bind
     ~@body))


(defmacro with-firefox-headless
  [[bind & [options]] & body]
  `(e/with-driver :firefox (assoc ~options :headless true) ~bind
     ~@body))


(defmacro with-edge-headless
  [[bind & [options]] & body]
  `(e/with-driver :edge (assoc ~options :headless true) ~bind
     ~@body))
