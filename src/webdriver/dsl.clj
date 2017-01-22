(ns webdriver.dsl
  (:require [webdriver.api :as api]
            [webdriver.keys :as keys]
            [webdriver.proc :as proc]
            [webdriver.client :as client]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.test :refer [is deftest]])
  (:import java.net.ConnectException)) ;; need that class?

;;
;; todos
;;
;; todo unused imports
;; todo variable bound checks?
;; todo: on exception return source code and screenshot
;; scenarios
;; multi-browser run in threads
;; wait for process
;; catch ConnectException when no server?
;; process logs
;; todo fill keys
;; skip decorator
;; conditinal decorator
;; with window decorator
;; todo add local html test
;; custom HTML files for tests
;; js clear local storage
;;

(def ^:dynamic *server*)
(def ^:dynamic *session*)
(def ^:dynamic *locator* "xpath")

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
;; locators
;;

(defmacro with-locator [locator & body]
  `(binding [*locator* ~locator]
     ~@body))

(defmacro with-css-selector [& body]
  `(with-locator "css selector"
     ~@body))

(defmacro with-link-text [& body]
  `(with-locator "link text"
     ~@body))

(defmacro with-partial-link-text [& body]
  `(with-locator "partial link text"
     ~@body))

(defmacro with-tag-name [& body]
  `(with-locator "tag name"
     ~@body))

(defmacro with-xpath [& body]
  `(with-locator "xpath"
     ~@body))

;;
;; navigation
;;

(defn go-url [url]
  (api/go *server* *session* url))

;;
;; url and title
;;

(defn get-title []
  (api/get-title *server* *session*))

(defn get-url []
  (api/get-current-url *server* *session*))

(defmacro with-title [name & body]
  `(let [~name (get-title)]
     ~@body))

(defmacro with-url [name & body]
  `(let [~name (get-url)]
     ~@body))

;;
;; css staff
;;

;; ;; todo refactor attrs

;; (defn css-prop-el [term prop]
;;   (as-> term $
;;     (api/find-element *server* *session* *locator* $)
;;     (api/get-element-css-value *server* *session* $ prop)))

;; (defn css-props-el [term props]
;;   (as-> term $
;;     (api/find-element *server* *session* *locator* $)
;;     (mapv #(api/get-element-css-value *server* *session* $ %) props)))

;; (defmacro with-css-prop-el [term prop & body]
;;   (let [$ '$]
;;     `(as-> term ~$
;;        (api/find-element *server* *session* *locator* ~$)
;;        (let [~prop ~$]
;;          ~@body))))

;; (defn css-prop [prop]
;;   (api/get-element-css-value *server* *session* *element* prop))

;; (defn css-props [props]
;;   (mapv #(api/get-element-css-value *server* *session* *element* %) props))

;; (defmacro with-css-prop [prop & body]
;;   `(let [~prop (css-prop ~prop)]
;;      ~@body))

;; ;; (defmacro with-css-props [props & body]
;; ;;   (let [bind-func (fn [prop] [prop `(css-prop ~(str prop))])
;; ;;         binds (->> props
;; ;;                    (map bind-func)
;; ;;                    (apply concat)
;; ;;                    vec
;; ;;                    vector)]
;; ;;     `(let ~@binds
;; ;;        ~body)))

;; ;; (defmacro with-css-props-el [term props & body]
;; ;;   `(with-element ~term
;; ;;      (with-css-props ~props
;; ;;        ~@body)))

;; ;; todo elemet size
;; ;; todo elemet rect
;; ;; element location
;; ;; resize
;; ;; position
;; ;; url-hash
;; ;; all the locators
;; ;; wait for (not) present/visible/enabled
;; ;;

;; ;;
;; ;; windowing
;; ;;

;; (defmacro with-window [handler & body]
;;   `(let [h# (api/get-window-handle *server* *session*)]
;;      (api/switch-to-window *server* *session* ~handler)
;;      (try
;;        ~@body
;;        (finally
;;          (api/switch-to-window *server* *session* h#)))))

;; (defmacro with-all-windows [& body]
;;   `(doseq [h# (api/get-window-handles *server* *session*)]
;;      (with-window h#
;;        ~@body)))

;; (defn close []
;;   (api/close-window *server* *session*))


;;
;; find elements
;;

(defmacro with-el [term el & body]
  `(let [~el (api/find-element *server* *session* *locator* ~term)]
     ~@body))

(defmacro with-els [term el & body]
  `(doseq [~el (api/find-elements *server* *session* *locator* ~term)]
     ~@body))

(defmacro with-el-from [parent term el & body]
  `(let [~el (api/find-element-from-element *server* *session* ~parent *locator* ~term)]
     ~@body))

(defmacro with-els-from [parent term el & body]
  `(doseq [~el (api/find-elements-from-element *server* *session* ~parent *locator* ~term)]
     ~@body))

;;
;; actions
;;

(defn fill-el [el text]
  (api/element-send-keys *server* *session* el text))

(defn fill [term text]
  (with-el term el
    (fill-el el text)))

(defn click-el [el]
  (api/element-click *server* *session* el))

(defn click [term]
  (with-el term el
    (click-el el)))

(defn clear-el [el]
  (api/element-clear *server* *session* el))

(defn clear [term]
  (with-el term el
    (clear-el el)))

(defn tap-el [el]
  (api/element-tap *server* *session* el))

(defn tap [term]
  (with-el term el
    (tap-el el)))

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
;; cookies
;;

;; ;; todo params
;; ;; todo without-cookie
;; ;; with-get-cooke
;; ;; multiple forms
;; ;; without-all-cookies?
;; ;;
;; (defmacro with-cookie [cookie & body]
;;   `(try
;;      (api/add-cookie *server* *session* cookie)
;;      ~@body
;;      (finally
;;        (api/delete-cookie-cookie *server* *session* cookie))))

;; ;;
;; ;; alerts
;; ;;

;; ;; todo alerts stuff

;; ;;
;; ;; screenshots
;; ;;

;; ;; todo compose filename func
;; (defn screenshot [filename]
;;   (api/take-screenshot *server* *session* filename))

;; (defn screenshot-el
;;   ([filename]
;;    (api/take-element-screenshot *server* *session* *element* filename))
;;   ([term filename]
;;    (with-element term
;;      (api/take-element-screenshot *server* *session* *element* filename))))

;; ;;
;; ;; scripts
;; ;;

;; ;; todo names? execute inject
;; (defn js-execute [script & args]
;;   (apply api/execute-script *server* *session* script args))

;; (defn js-inject-script [url]
;;   (let [script (str "var s = document.createElement('script');"
;;                     "s.type = 'text/javascript';"
;;                     "s.src = arguments[0];"
;;                     "document.head.appendChild(s);")]
;;     (js-execute script url)))

;; ;;
;; ;; predicates
;; ;;

;; ;; todo simplify
;; ;; todo form for []?
;; (defn exists? ;; todo one form
;;   ([]
;;    (try+
;;     (api/get-element-tag-name *server* *session* *element*)
;;     true
;;     (catch [:status 404] _
;;       false)))
;;   ([term]
;;    (with-element term
;;      (try+
;;       (api/get-element-tag-name *server* *session* *element*)
;;       true
;;       (catch [:status 404] _
;;         false)))))

;; (defn enabled?
;;   ([]
;;    (api/is-element-enabled *server* *session* *element*))
;;   ([term]
;;    (with-element term
;;      (api/is-element-enabled *server* *session* *element*))))

;; (defn displayed?
;;   ([]
;;    (api/is-element-displayed *server* *session* *element*))
;;   ([term]
;;    (with-element term
;;      (api/is-element-displayed *server* *session* *element*))))

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

;; (defn wait-for-element-exists [term & args]
;;   ;; todo multi-form
;;   (apply wait-for-predicate
;;          (partial exists? term)
;;          args))

;; (defn wait-for-element-enabled [term & args]
;;   ;; todo multi-form
;;   (apply wait-for-predicate
;;          (partial enabled? term)
;;          args))

;; (defn wait-for-element-displayed [term & args]
;;   ;; todo multi-form
;;   (apply wait-for-predicate
;;          (partial displayed? term)
;;          args))

;; ;; todo exception decorator
;; (defn running? [host port]
;;   (try+
;;    (api/status {:url (format "http://%s:%d" host port)})
;;    true
;;    (catch ConnectException _
;;      false)))

;; (defn wait-for-running [host port & args]
;;   (apply wait-for-predicate
;;          (partial running? host port)
;;          args))

;; ;;
;; ;; keys and input
;; ;;

;; (defn fill
;;   ([text]
;;    (api/element-send-keys *server* *session* *element* text))
;;   ([term text]
;;    (with-element term
;;      (api/element-send-keys *server* *session* *element* text))))

;; (defn make-fill-key [key]
;;   (-> fill flip (partial key)))

;; (def enter (make-fill-key keys/enter))
;; (def backspace (make-fill-key keys/backspace))
;; (def up (make-fill-key keys/up))
;; (def right (make-fill-key keys/right))
;; (def down (make-fill-key keys/down))
;; (def left (make-fill-key keys/left))

;; (defn fill-human [text]
;;   "Inputs text like we typically do: with random delays and corrections."
;;   ;; todo multiple arguments
;;   ;; todo random values
;;   ;; todo weights
;;   ;; todo multi-form
;;   (doseq [key text]
;;     (when (< (rand) 0.3)
;;       (fill \A)
;;       (wait 0.3)
;;       (backspace))
;;     (fill key)
;;     (wait 0.2)))

;; ;; todo multi-form
;; ;; todo fill form human
;; ;; todo submit form
;; (defn fill-form [form]
;;   (doseq [[field text] form]
;;     (let [term (format "//input[@name='%s']" (name field))]
;;       (with-element term
;;         (fill text)))))

;; ;;
;; ;; proceses
;; ;;

;; todo handle exceptions
;; check alive
;; (defmacro with-process [host port & body]
;;   `(let [proc# (proc/run-gecko ~host ~port)]
;;      (wait 1) ;; todo what time to wait?
;;      (when-not (and (nil? (proc/exit-code proc#)) ;; todo sep func for that
;;                     (proc/alive? proc#))
;;        (throw+ {:type ::process-error})) ;; error
;;      ;; (wait-for-running ~host ~port)
;;      (try
;;       ~@body
;;       (finally
;;         (proc/kill proc#)))))

(defn make-server-url [host port]
  (format "http://%s:%d" host port))

(defn make-server [host port]
  {:host host
   :port port
   :url (make-server-url host port)})

(defmacro with-server [host port & body]
  `(binding [*server* (make-server ~host ~port)]
     ~@body))

;; (defmacro with-server-multi [servers & body]
;;   `(doseq [[host# port#] ~servers]
;;      (binding [*server* (make-server host# port#)]
;;        ~@body)))

;; ;; (defmacro with-start [host port & body]
;; ;;   `(with-server ~host ~port
;; ;;      (with-process ~host ~port
;; ;;        ~@body)))

;; ;; (defmacro with-start-multi [connections & body]
;; ;;   `(doseq [[host# port#] ~connections]
;; ;;      (with-server host# port#
;; ;;        (with-process host# port#
;; ;;          ~@body))))

;; ;; todo multi-futures


;;
;; element attributes
;;

(defn attr-el [el name]
  (api/get-element-attribute *server* *session* el name))

(defn attr [term name]
  (with-el term el
    (api/get-element-attribute *server* *session* el name)))

(defmacro with-attr-el [el name & body]
  `(let [~name (attr-el ~el ~(str name))]
     ~@body))

(defmacro with-attr [term name & body]
  `(with-el ~term el#
     (with-attr-el el# ~name
       ~@body)))

(defmacro with-attrs-el [el names & body]
  (let [func (fn [name] `(attr-el ~el ~(str name)))
        forms (map func names)
        binds (-> names
                  (interleave forms)
                  vec
                  vector)]
    `(let ~@binds
       ~@body)))

(defmacro with-attrs [term names & body]
  `(with-el ~term el#
     (with-attrs-el el# ~names
       ~@body)))

;; (defn el-attr [attr]
;;   (api/get-element-attribute *server* *session* *element* attr))

(defmacro with-el-attrs [attrs & body]
  (let [pair-func (fn [attr]
                    (let [attr-str (str attr)]
                      [attr `(el-attr ~attr-str)]))
        binds (->> attrs
                   (map pair-func)
                   (apply concat)
                   vec
                   vector)]
    `(let ~@binds
       ~@body)))

;; (defmacro with-el-attr [attr & body]
;;   `(with-el-attrs [~attr]
;;      ~@body))

;; (defn el-prop [prop]
;;   (api/get-element-property *server* *session* *element* prop))

;; (defmacro with-el-props [props & body]
;;   (let [pair-func (fn [prop]
;;                     (let [prop-str (str prop)]
;;                       [prop `(el-prop ~prop-str)]))
;;         binds (->> props
;;                    (map pair-func)
;;                    (apply concat)
;;                    vec
;;                    vector)]
;;     `(let ~@binds
;;        ~@body)))

;; ;; todo for multiple prop
;; ;; todo namin
;; ;; todo the same for attr
;; (defmacro with-prop [term prop & body]
;;   `(with-element [~term]
;;      (with-el-prop [~prop]
;;        ~@body)))

;; (defmacro with-el-prop [prop & body]
;;   `(with-el-props [~prop]
;;      ~@body))


(deftest simple-test
  (let [host "127.0.0.1"
        port (random-port) ;; 4444 ;; 8910
        args ["geckodriver" "--host" host "--port" port]
        capabilities {}
        html "<input class=\"input__control input__input\" tabindex=\"2\" autocomplete=\"off\" autocorrect=\"off\" autocapitalize=\"off\" spellcheck=\"false\" aria-autocomplete=\"list\" aria-label=\"Запрос\" id=\"text\" maxlength=\"400\" name=\"text\">"
        input "//input[@id='text']"]

    ;; with-start host port
    (proc/with-proc p [args]
      (with-server host port
        (wait 1) ;; todo fix that wait-for-server mb?
        (with-session capabilities
          (client/with-pool {}
            (go-url "http://ya.ru")
            (with-xpath
              (wait 2)
              (with-el input el
                (fill-el el "test")
                (with-attr-el el maxlength
                  (is (= maxlength "400"))))
              (fill input "test")
              (with-attr input maxlength
                (is (= maxlength "400")))
              (with-attrs input [name class tabindex
                                 autocomplete maxlength]
                (is (= name "text"))
                (is (= class "input__control input__input"))
                (is (= tabindex "2"))
                (is (= autocomplete "off"))
                (is (= maxlength "400")))
              ;; (wait-for-element-exists input)
              ;; name shorter
              ;; (with-element input
              ;;   ;; (with-el-prop outerHTML
              ;;   ;;   (is (= outerHTML html)))
              ;;   ;; (with-el-props [outerHTML innerHTML]
              ;;   ;;   (is (= outerHTML html))
              ;;   ;;   (is (= innerHTML "")))
              ;;   ;; (with-el-attr name
              ;;   ;;   (is (= name "text")))
              ;;   ;; (with-el-attrs [name class tabindex
              ;;   ;;                 autocomplete maxlength]
              ;;   ;;   (is (= name "text"))
              ;;   ;;   (is (= class "input__control input__input"))
              ;;   ;;   (is (= tabindex "2"))
              ;;   ;;   (is (= autocomplete "off"))
              ;;   ;;   (is (= maxlength "400")))
              ;;   (fill "Clojure"))
              ;; (with-element "//form"
              ;;   (fill-form {:text "ho-ho-ho"}))
              ))
          (wait 2)
          (is 1))))))

;; (defn foo []
;;   (doseq [foo [1 2 3  2 2 2 2 2 2 2 2 2]]
;;     (future (run-tests))))
