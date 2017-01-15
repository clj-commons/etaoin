(ns webdriver.dsl
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [webdriver.api :as api]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.test :refer [is deftest]]))

(def ^:dynamic *session*)
(def ^:dynamic *element*)
(def ^:dynamic *locator*)

(defmacro with-locator [locator & body]
  `(with-bindings {#'*locator* ~locator}
     ~@body))

(defmacro with-xpath [& body]
  `(with-locator "xpath"
     ~@body))

(defn make-selector [term]
  [*locator* term])

(defn find-element [term]
  (api/find-element *session* (make-selector term)))

(defn find-element-from-element [element term]
  (api/find-element-from-element *session* element (make-selector term)))

(defn element-value [element text]
  (api/element-value *session* element text))

(defmacro with-element [term & body]
  `(if (bound? #'*element*)
     (with-bindings {#'*element* (find-element-from-element *element* ~term)}
       ~@body)
     (with-bindings {#'*element* (find-element ~term)}
       ~@body)))

(defn go-url [url]
  (api/go-url *session* url))

(defn get-title []
  (api/get-title *session*))

(defn get-url []
  (api/get-url *session*))

(defn fill
  ([text] (fill *element* text))
  ([term text]
   (let [element (api/find-element *session* (make-selector term))]
     (element-value element text))))

(defn click
  ([]
   (api/element-click *session* *element*))
  ([term]
   (let [element (api/find-element *session* term)]
     (api/element-click *session* element))))

(defmacro with-session [host port capabilities & body]
  `(let [url# (format "http://%s:%d" ~host ~port)
         server# {:url url#}
         session# (api/new-session server# ~capabilities)]
     (with-bindings {#'*session* (merge session# server#)}
       (let [result# (do ~@body)] ;; todo try catch
         (api/delete-session *session*)
         result#))))

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

(deftest foo
  (with-session "127.0.0.1" 4444 {}
    (go-url "http://ya.ru")
    (wait 7)
    (with-xpath
      (fill "//input[@id=\"text\"]" "test"))
    (wait 5)
    (is 1)))

;; (defn backspace [])
;; (defn enter [] \uE007)

;; (defmacro with-server [host port & body]
;;   `(with-bindings {#'*server* (make-server host port)}
;;      ~@body))

;; (defmacro with-server [host port & body]
;;   `(with-bindings {#'*server* (make-server host port)}
;;      ~@body))


;; (with-session {:host "127.0.0.1"
;;                :port 4444
;;                :capabilities api/default-capabilities}
;;     (url "http://ya.ru")
;;     (back)
;;     (forward)
;;     (wait 3)
;;     (wait-for-element-visible "input")
;;     (inject-script "http://ya.ru/test.js")
;;     (click "test")
;;     (with-xpath
;;       (with-element "form"
;;         (fill "email" "test@test.com")
;;         (fill "password" "**********")
;;         (enter "password")
;;         (pack-space "password")
;;         (click "submit")))
;;     (print (get-title))
;;     (print (get-url)))

;; ;; (defmacro with-server [host port & body]
;; ;;   `(let [server# (make-server ~host ~port)
;; ;;          session# (api/session-create server#)]
;; ;;      (with-bindings {#'*server* server#
;; ;;                      #'*session* session#}
;; ;;        (let [result# (do ~@body)]
;; ;;          (api/session-delete server# session#)
;; ;;          result#))))


;; (defn make-selector [term]
;;   [*locator* term])

;; (defn el-find [term]
;;   (api/element-find *server* *session*
;;                     (make-selector term)))

;; ;; (def go-url (partial api/go-url *server* *session*))

;; ;; (defn url [url]
;; ;;   (api/go-url *server* *session* url))

;; ;; (defn back []
;; ;;   (api/go-back *server* *session*))


;; ;; (defn click [selector]
;; ;;   (let [element (-> (api/element-find server session selector))]
;; ;;     (api/element-click server session)))


;; (defn element-click [server session element]
;;   ;; (api browser
;;   ;;      :post
;;   ;;      [:session (-> browser :session :sessionId) :element element :click])
;;   )

;; (defn make-server [host port]
;;   {:host host
;;    :port port
;;    :url (format "http://%s:%d" host port)})

;; (defn make-browser [host port]
;;   (let [server (make-server host port)
;;         browser {:server server}
;;         session (api/session-create browser)]
;;     (assoc browser :session session)))

;; (def url api/go-url)
;; (def back api/go-back)
;; (def fwd api/go-fwd)
;; (def end api/session-delete)

;; (def get-title api/get-title)



;; (defn fill [text])

;; (defn fill-in [browser selector text]
;;   (let [element (api/element-find browser selector)]
;;     (api/element-value browser element text)))

;; ;; (defn title-matches [browser re-title]
;; ;;   (re-matches re-title (api/get-title browser)))

;; ;; (defmacro is-title-matches [browser re-title]
;; ;;   `(let [title# (api/get-title ~browser)]
;; ;;      (re-matches ~re-title title#)))

;; ;; (def is-title-matches (comp title-matches (fn [x] (is x))))

;; (def wait-timeout 30)

;; (defn element-visible? [browser selector]
;;   "https://www.w3.org/TR/webdriver/#element-displayedness"
;;   (try+
;;    (let [element (api/element-find browser selector)]
;;      (cond
;;        (api/element-attribute browser element :hidden) false
;;        :else true))
;;    (catch [:status 404] _
;;      false)))

;; (defn wait-for-element-visible
;;   [browser selector & {:keys [timeout delta]
;;                        :or {timeout 30 delta 1}}]
;;   (loop [times 0
;;          time-rest timeout]
;;     (when (< time-rest 0)
;;       (throw+ :todo)) ;; todo
;;     (when-not (element-visible?
;;                browser selector)
;;       (wait delta)
;;       (recur (inc times)
;;              (- time-rest delta)))))

;; (deftest foo
;;   (let [browser (make-browser "127.0.0.1" 4444)
;;         input "//input[@id=\"text\"]"
;;         button "//button[@type=\"submit\"]"]
;;     (url browser "http://ya.ru")
;;     (let [title (get-title browser)]
;;       (is (= title "Яндекс")))
;;     (wait-for-element-visible browser input :timeout 10 :delta 1)
;;     (fill-in browser input "Clojure official site\uE007")

;;     ;; (end browser)
;;     ))

;; ;; (is-title-matches #"Яндекс2")
;; ;; (-> (title-matches #"Яндекс2") is)

;; ;; (defmacro with-server [host port & body]
;; ;;   `(let [server# (make-server ~host ~port)
;; ;;          session# (api/session-create server#)]
;; ;;      (with-bindings {#'*server* server#
;; ;;                      #'*session* session#}
;; ;;        (let [result# (do ~@body)]
;; ;;          (api/session-delete server# session#)
;; ;;          result#))))

;; ;; (with-server "127.0.0.1" 4444 (url "http://ya.ru") (back))
