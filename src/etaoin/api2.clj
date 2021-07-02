(ns etaoin.api2
  "
  Better syntax for some API that cannot be fixed
  without breaking them.
  "
  (:require
   [etaoin.api :as api]))


(defmacro with-firefox
  [[bind & [options]] & body]
  `(api/with-driver :firefox ~options ~bind
     ~@body))


(defmacro with-chrome
  [[bind & [options]] & body]
  `(api/with-driver :chrome ~options ~bind
     ~@body))


(defmacro with-edge
  [[bind & [options]] & body]
  `(api/with-driver :edge ~options ~bind
     ~@body))


(defmacro with-phantom
  [[bind & [options]] & body]
  `(api/with-driver :phantom ~options ~bind
     ~@body))


(defmacro with-safari
  [[bind & [options]] & body]
  `(api/with-driver :safari ~options ~bind
     ~@body))


(defmacro with-chrome-headless
  [[bind & [options]] & body]
  `(api/with-driver :chrome (assoc ~options :headless true) ~bind
     ~@body))


(defmacro with-firefox-headless
  [[bind & [options]] & body]
  `(api/with-driver :firefox (assoc ~options :headless true) ~bind
     ~@body))


(defmacro with-edge-headless
  [[bind & [options]] & body]
  `(api/with-driver :edge (assoc ~options :headless true) ~bind
     ~@body))
