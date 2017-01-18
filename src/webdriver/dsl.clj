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
;; process logs
;; skip decorator
;; conditinal decorator
;; with window decorator
;; todo add local html test
;;

(def ^:dynamic *server*)
(def ^:dynamic *session*)
(def ^:dynamic *element*)
(def ^:dynamic *locator*)

;;
;; tools
;;

(defn flip [f]
  (fn [& args]
    (apply f (reverse args))))

(defn random-port []
  (let [max-port 65536
        offset 1024]
    (+ (rand-int (- max-port offset))
       offset)))

;;
;; selectors
;;

(defmacro with-locator [locator & body]
  `(binding [*locator* ~locator]
     ~@body))

(defmacro with-xpath [& body]
  `(with-locator "xpath"
     ~@body))

;;
;; windowing
;;

(defmacro with-window [handler & body]
  `(let [h# (api/get-window-handle *server* *session*)]
     (api/switch-to-window *server* *session* ~handler)
     (try
       ~@body
       (finally
         (api/switch-to-window *server* *session* h#)))))

(defmacro with-all-windows [& body]
  `(doseq [h# (api/get-window-handles *server* *session*)]
     (with-window h#
       ~@body)))

(defn close []
  (api/close-window *server* *session*))

;;
;; navigation
;;

(defn go-url [url]
  (api/go-url *server* *session* url))

;;
;; url and title
;;

(defn get-title []
  (api/get-title *server* *session*))

(defn get-url []
  (api/get-url *server* *session*))

(defmacro with-title [name & body]
  `(let [~name (get-title)]
     ~@body))

(defmacro with-url [name & body]
  `(let [~name (get-url)]
     ~@body))

;;
;; elements
;;

(defmacro with-element [term & body]
  `(if (bound? #'*element*)
     (binding [*element* (api/find-element-from-element
                          *server* *session* *element* *locator* ~term)]
       ~@body)
     (binding [*element* (api/find-element
                          *server* *session* *locator* ~term)]
       ~@body)))

(defmacro with-elements [term & body]
  `(if (bound? #'*element*)
     (doseq [element# (api/find-element-from-element
                       *server* *session* *element* *locator* ~term)]
       (binding [*element* element#]
         ~@body))
     (doseq [element# (api/find-elements
                       *server* *session* *locator* ~term)]
       (binding [*element* element#]
         ~@body))))

;;
;; keys and input
;;

(defn fill
  ([text]
   (api/element-value *server* *session* *element* text))
  ([term text]
   (with-element term
     (api/element-value *server* *session* *element* text))))

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

(defmacro with-session [capabilities & body]
  `(binding [*session* (api/new-session *server* ~capabilities)]
     (try
       ~@body
       (finally
         (api/delete-session *server* *session*)))))

;;
;; clicks
;;

(defn click
  ([]
   (api/element-click *server* *session* *element*))
  ([term]
   (with-element term
     (api/element-click *server* *session* *element*))))

;;
;; scripts
;;

(defn execute-js [script & args]
  (apply api/execute-script *server* *session* script args))

(defn inject-js [url]
  (let [script (str "var s = document.createElement('script');"
                    "s.type = 'text/javascript';"
                    "s.src = arguments[0];"
                    "document.head.appendChild(s);")]
    (execute-js script url)))

;;
;; predicates
;;

(defn exists? ;; todo one form
  ([]
   (try+
    (api/element-tag-name *server* *session* *element*)
    true
    (catch [:status 404] _
      false)))
  ([term]
   (with-element term
     (try+
      (api/element-tag-name *server* *session* *element*)
      true
      (catch [:status 404] _
        false)))))

(defn enabled?
  ([]
   (api/element-enabled *server* *session* *element*))
  ([term]
   (with-element term
     (api/element-enabled *server* *session* *element*))))

(defn visible? []
  ;; todo C. Element Displayedness
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

;;
;; proceses
;;

;; todo handle exceptions
;; check alive
(defmacro with-process [host port & body]
  `(let [proc# (proc/run-gecko ~host ~port)]
     (wait 1) ;; todo what time to wait?
     (when-not (and (nil? (proc/exit-code proc#)) ;; todo sep func for that
                    (proc/alive? proc#))
       (throw+ {:type ::process-error})) ;; error
     ;; (wait-for-running ~host ~port)
     (try
      ~@body
      (finally
        (proc/kill proc#)))))

(defn make-server-url [host port]
  (format "http://%s:%d" host port))

(defn make-server [host port]
  {:host host
   :port port
   :url (make-server-url host port)})

(defmacro with-server [host port & body]
  `(binding [*server* (make-server ~host ~port)]
     ~@body))

(defmacro with-server-multi [servers & body]
  `(doseq [[host# port#] ~servers]
     (binding [*server* (make-server host# port#)]
       ~@body)))

(defmacro with-start [host port & body]
  `(with-server ~host ~port
     (with-process ~host ~port
       ~@body)))

(defmacro with-start-multi [connections & body]
  `(doseq [[host# port#] ~connections]
     (with-server host# port#
       (with-process host# port#
         ~@body))))

(deftest simple-test
  (let [host "127.0.0.1"
        port (random-port) ;; 4444 ;; 8910
        capabilities {}
        input "//input[@id=\"text\"]"]
    (with-start host port
      (with-session capabilities
        (go-url "http://ya.ru")
        (wait-for-element-exists input)
        (with-xpath
          (with-element input
            (fill-human "Clojure")
            (enter)))
        (wait 1)
        (is 1)))))

(defn foo []
  (doseq [foo [1 2 3  2 2 2 2 2 2 2 2 2]]
    (future (run-tests)))
  )
