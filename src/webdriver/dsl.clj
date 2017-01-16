(ns webdriver.dsl
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [webdriver.api :as api]
            [webdriver.keys :as keys]
            [webdriver.proc :as proc]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.test :refer [is deftest run-tests]])
  (:import java.net.ConnectException))

;;
;; todos
;;
;; todo variable bound checks?
;; todo: on exception return source code and screenshot
;; (inject-script "http://ya.ru/test.js")
;; scenarios
;; multi-browser run in threads
;; wait for process
;; HTTP parse error json
;; catch ConnectException when no server?
;; http connection pool
;;

(def ^:dynamic *session*)
(def ^:dynamic *element*)
(def ^:dynamic *locator*)

;;
;; tools
;;

(defn flip [f]
  (fn [& args]
    (apply f (reverse args))))

;;
;; selectors
;;

(defmacro with-locator [locator & body]
  `(binding [*locator* ~locator]
     ~@body))

(defmacro with-xpath [& body]
  `(with-locator "xpath"
     ~@body))

(defn make-selector [term]
  [*locator* term])

;;
;; navigation
;;

(defn go-url [url]
  (api/go-url *session* url))

(defn get-title []
  (api/get-title *session*))

(defn get-url []
  (api/get-url *session*))

;;
;; elements
;;

(defmacro with-element [term & body]
  `(if (bound? #'*element*)
     (binding [*element* (api/find-element-from-element
                          *session* *element*
                          (make-selector ~term))]
       ~@body)
     (binding [*element* (api/find-element
                          *session*
                          (make-selector ~term))]
       ~@body)))

;;
;; keys and input
;;

(defn fill
  ([text]
   (api/element-value *session* *element* text))
  ([term text]
   (with-element term
     (api/element-value *session* *element* text))))

(defn make-fill-key [key]
  (-> fill flip (partial key)))

(def enter(make-fill-key keys/enter))
(def backspace (make-fill-key keys/backspace))
(def up (make-fill-key keys/up))
(def right (make-fill-key keys/right))
(def down (make-fill-key keys/down))
(def left (make-fill-key keys/left))

(defn fill-human [text]
  "Inputs text like we typically do: with random delays and corrections."
  ;; todo multiple arguments
  ;; todo random values
  ;; todo weights
  ;; todo multi-form
  (doseq [key text]
    (when (< (rand) 0.3)
      (fill \A)
      (wait 0.3)
      (backspace))
    (fill key)
    (wait 0.2)))

;;
;; session
;;

(defmacro with-session [host port capabilities & body]
  `(let [url# (format "http://%s:%d" ~host ~port)
         server# {:url url#}
         session# (api/new-session server# ~capabilities)]
     (binding [*session* (merge session# server#)]
       (let [result# (do ~@body)] ;; todo try catch
         (api/delete-session *session*)
         result#))))

;;
;; clicks
;;

(defn click
  ([]
   (api/element-click *session* *element*))
  ([term]
   (with-element term
     (api/element-click *session* *element*))))

;;
;; predicates
;;

(defn exists?
  ([]
   (try+
    (api/element-tag-name *session* *element*)
    true
    (catch [:status 404] _
      false)))
  ([term]
   (with-element term
     (try+
      (api/element-tag-name *session* *element*)
      true
      (catch [:status 404] _
        false)))))

(defn enabled?
  ([]
   (api/element-enabled *session* *element*))
  ([term]
   (with-element term
     (api/element-enabled *session* *element*))))

(defn visible? []
  )

;;
;; wait functions
;;

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

(defn wait-for-predicate
  [predicate & {:keys [timeout delta]
                :or {timeout 30 delta 1}}]
  (loop [times 0
         time-rest timeout]
    (when (< time-rest 0)
      (throw+ {:type ::time-has-left})) ;; todo error data
    (when-not (predicate)
      (wait delta)
      (recur (inc times)
             (- time-rest delta)))))

(defn wait-for-element-exists [term & args]
  ;; todo multi-form
  (apply wait-for-predicate
         (partial exists? term)
         args))

(defn wait-for-element-enabled [term & args]
  ;; todo multi-form
  (apply wait-for-predicate
         (partial enabled? term)
         args))

(defn wait-for-element-visible [term & args]
  ;; todo multi-form
  (apply wait-for-predicate
         (partial visible? term)
         args))

;; todo exception decorator
(defn running? [host port]
  (try+
   (api/status {:url (format "http://%s:%d" host port)})
   true
   (catch ConnectException _
     false)))

(defn wait-for-running [host port & args]
  (apply wait-for-predicate
         (partial running? host port)
         args))

;; todo handle exceptions
;; check alive
(defmacro with-process [host port & body]
  `(let [proc# (proc/run-gecko ~host ~port)]
     (wait 1) ;; todo wait time
     (when-not (and (nil? (proc/exit-code proc#))
                    (proc/alive? proc#))
       (throw+ {:type ::process-error})) ;; error
     (wait-for-running ~host ~port)
     ~@body
     (proc/kill proc#)))

(defmacro with-browsers [browsers & body]
  `(doseq [browser# ~browsers]
     (with-process browser#
       ~@body)))

(defmacro with-server [& body]
  `(let []
     ~@body))

(defmacro with-start [host port & body]
  `(with-process ;; todo
     (with-server host# port#
       ~@body)))

(defmacro with-start-multi [connections & body]
  `(doseq [[host# port#] ~connections]
     (with-process ;; todo
       (with-server host# port#
         ~@body))))

(defmacro with-connect [host port & body]
  `(with-server host# port#
     ~@body))

(defmacro with-connect-multi [connections & body]
  `(doseq [[host# port#] ~connections]
     (with-server host# port#
       ~@body)))

(deftest simple-test
  (let [host "127.0.0.1"
        port (+ 1024 (rand-int 50000)) ;; 4444 ;; 8910 ;; todo port function
        capabilities {}
        input "//input[@id=\"text\"]"]

    ;; (with-browsers []
    ;;   (with-session host port capabilities
    ;;     (go-url "http://ya.ru")
    ;;     (wait-for-element-exists input)
    ;;     (with-xpath
    ;;       (with-element input
    ;;         (fill-human "Clojure")
    ;;         (enter)))
    ;;     (wait 2)
    ;;     (is 1)))

    (with-process host port
      (with-session host port capabilities
        (go-url "http://ya.ru")
        (wait-for-element-exists input)
          (with-xpath
          (with-element input
            (fill-human "Clojure")
            (enter)))
        ;; (wait 2)
        (is 1))
      ;; (with-session host port capabilities
      ;;   (go-url "http://ya.ru")
      ;;   (wait-for-element-exists input)
      ;;   (with-xpath
      ;;     (with-element input
      ;;       (fill-human "Google")
      ;;       (enter)))
      ;;   (wait 2)
      ;;   (is 1))
      )))

(defn foo []
  (doseq [foo [1 2 3  2 2 2 2 2 2 2 2 2]]
    (future (run-tests)))
  )
