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
;; process logs
;; todo fill keys
;; skip decorator
;; todo add local html test
;; custom HTML files for tests
;; js clear local storage
;; todo elemet size
;; todo elemet rect
;; element location
;; resize
;; position
;; url-hash
;; check if process is alive
;; wait for (not) present/visible/enabled
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

(defn back []
  (api/back *server* *session*))

(defn forward []
  (api/forward *server* *session*))

(defn refresh []
  (api/refresh *server* *session*))

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
;; windows
;;

(defn close []
  (api/close-window *server* *session*))

(defn maximize []
  (api/maximize-window *server* *session*))

(defn fullscreen []
  (api/fullscreen-window *server* *session*))

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

;;
;; geometry
;;

(defn get-el-rect-el [el]
  (api/get-element-rect *server* *session* el))

(defn get-el-rect [term]
  (with-el term el
    (get-el-rect-el el)))

(defmacro with-el-rect-el [el bind-form & body]
  `(let [~bind-form (get-el-rect-el ~el)]
     ~@body))

(defmacro with-el-rect [term bind-form & body]
  `(with-el ~term el#
     (let [~bind-form (get-el-rect-el el#)]
     ~@body)))

;;
;; css stuff
;;

(defn css-el [el name]
  (api/get-element-css-value *server* *session* el name))

(defn css [term name]
  (with-el term el
    (css-el el name)))

(defmacro with-css-el [el name & body]
  `(let [~name (css-el ~el ~(str name))]
     ~@body))

(defmacro with-css [term name & body]
  `(with-el ~term el#
     (with-css-el el# ~name
       ~@body)))

(defmacro with-csss-el [el names & body]
  (let [func (fn [name] `(css-el ~el ~(str name)))
        forms (map func names)
        binds (-> names
                  (interleave forms)
                  vec
                  vector)]
    `(let ~@binds
       ~@body)))

(defmacro with-csss [term names & body]
  `(with-el ~term el#
     (with-csss-el el# ~names
       ~@body)))

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

(defn with-el-active [el & body]
  `(let [~el (api/get-active-element *server* *session*)]
     ~@body))

;;
;; actions
;;

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

;;
;; predicates
;;

(defmacro with-exception [catch fallback & body]
  `(try+
    ~@body
    (catch ~catch ~(quote _)
      ~fallback)))

(defmacro with-404 [& body]
  `(with-exception [:status 404] false
     ~@body))

(defmacro with-conn-error [& body]
  `(with-exception ConnectException false
     ~@body))

(defn exists-el [el]
  (with-404
    (api/get-element-tag-name *server* *session* el)
    true))

(defn exists [term]
  (with-404
    (with-el term el
      true)))

(defn enabled-el [el]
  (api/is-element-enabled *server* *session* el))

(defn enabled [term]
  (with-el term el
    (enabled-el el)))

(defn visible-el [el]
  (with-404
    (api/is-element-displayed *server* *session* el)))

(defn visible [term]
  (with-404
    (with-el term el
      (visible-el el))))

(defn running []
  (with-conn-error
    (api/status *server*)))

;;
;; wait functions
;;

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

(defn wait-for-predicate
  [predicate & {:keys [timeout poll]
                :or {timeout 10 poll 0.5}}]
  (loop [times 0
         time-rest timeout]
    (when (< time-rest 0)
      (throw+ {:type :webdriver/timeout
               :timeout timeout
               :poll poll
               :times times
               :predicate predicate}))
    (when-not (predicate)
      (wait poll)
      (recur (inc times)
             (- time-rest poll)))))

(defn wait-enabled-el [el & args]
  (apply wait-for-predicate #(enabled-el el) args))

(defn wait-enabled [term & args]
  (apply wait-for-predicate #(enabled term) args))

(defn wait-exists-el [el & args]
  (apply wait-for-predicate #(exists-el el) args))

(defn wait-exists [term & args]
  (apply wait-for-predicate #(exists term) args))

(defn wait-visible-el [el & args]
  (apply wait-for-predicate #(visible-el el) args))

(defn wait-visible [term & args]
  (apply wait-for-predicate #(visible term) args))

(defn wait-running [& args]
  (apply wait-for-predicate running args))

;;
;; keys and input
;;

(defn fill-el [el text]
  (api/element-send-keys *server* *session* el text))

(defn fill [term text]
  (with-el term el
    (fill-el el text)))

(defn enter-el [el]
  (fill-el el keys/enter))

(defn enter [term]
  (fill term keys/enter))

(defn backspace-el [el]
  (fill-el el keys/backspace))

(defn backspace [term]
  (fill term keys/backspace))

;; (defn make-fill-key [key]
;;   (-> fill flip (partial key)))

;; (def enter (make-fill-key keys/enter))
;; (def backspace (make-fill-key keys/backspace))
;; (def up (make-fill-key keys/up))
;; (def right (make-fill-key keys/right))
;; (def down (make-fill-key keys/down))
;; (def left (make-fill-key keys/left))

(defn fill-human-el [el text & {:keys [mistake pause]
                                :or {mistake 0.3 pause 0.2}}]
  (let [rand-char #(-> 26 rand-int (+ 97) char)
        wait-key #(let [r (rand)]
                    (wait (if (> r pause) pause r)))]
    (doseq [key text]
      (when (< (rand) mistake)
        (fill-el el (rand-char))
        (wait-key)
        (backspace-el el)
        (wait-key))
      (fill-el el key)
      (wait-key))))

(defn fill-human [term text]
  (with-el term el
    (fill-human-el el text)))

(defn fill-form-el [el-form form]
  (doseq [[field value] form]
    (let [term (format "//input[@name='%s']" (name field))]
      (with-el-from el-form term el-input
        (fill-el el-input (str value))))))

(defn fill-form [term form]
  (with-el term el-form
    (fill-form-el el-form form)))

;; ;; todo submit form
;; ;; todo multi-form
;; ;; todo fill form human
;; ;; todo submit form
;; (defn fill-form [form]
;;   (doseq [[field text] form]
;;     (let [term (format "//input[@name='%s']" (name field))]
;;       (with-element term
;;         (fill text)))))

;;
;; proceses
;;

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
    (attr-el el name)))

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

;;
;; element properties
;;

(defn prop-el [el name]
  (api/get-element-property *server* *session* el name))

(defn prop [term name]
  (with-el term el
    (prop-el el name)))

(defmacro with-prop-el [el name & body]
  `(let [~name (prop-el ~el ~(str name))]
     ~@body))

(defmacro with-prop [term name & body]
  `(with-el ~term el#
     (with-prop-el el# ~name
       ~@body)))

(defmacro with-props-el [el names & body]
  (let [func (fn [name] `(prop-el ~el ~(str name)))
        forms (map func names)
        binds (-> names
                  (interleave forms)
                  vec
                  vector)]
    `(let ~@binds
       ~@body)))

(defmacro with-props [term names & body]
  `(with-el ~term el#
     (with-props-el el# ~names
       ~@body)))

;;
;; tests
;;

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
        (wait-running)
        (with-session capabilities
          (client/with-pool {}
            (go-url "http://ya.ru")
            (with-xpath
              (wait-visible input)
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
              (with-props input [outerHTML innerHTML]
                (is (= outerHTML html))
                (is (= innerHTML "")))
              (with-csss input [display font-size height border-right-width border-collapse]
                (is (= display "inline"))
                ;; (is (= font-size "19px")) ;; todo ?
                (is (= height "46px"))
                (is (= border-right-width "40px"))
                (is (= border-collapse "collapse")))
              (fill-form "//form" {:text "sdfsdfsdfsdfs"})
              (fill-human input "I dunno why I do that.")
              (with-el-rect input {:keys [x y width height]}
                (is (= x 222.0))
                (is (= y 295.0))
                (is (= width 692.0))
                (is (= height 46.0)))))
          (wait 2)
          (is 1))))))

;; (defn foo []
;;   (doseq [foo [1 2 3  2 2 2 2 2 2 2 2 2]]
;;     (future (run-tests))))
