(ns webdriver.dsl
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [webdriver.api :as api]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.test :refer [is deftest]]))

(def ^:dynamic *server*)
(def ^:dynamic *session*)

;; (def go-url (partial api/go-url *server* *session*))

;; (defn url [url]
;;   (api/go-url *server* *session* url))

;; (defn back []
;;   (api/go-back *server* *session*))


;; (defn click [selector]
;;   (let [element (-> (api/element-find server session selector))]
;;     (api/element-click server session)))


(defn element-click [server session element]
  ;; (api browser
  ;;      :post
  ;;      [:session (-> browser :session :sessionId) :element element :click])
  )

(defn make-server [host port]
  {:host host
   :port port
   :url (format "http://%s:%d" host port)})

(defn make-browser [host port]
  (let [server (make-server host port)
        browser {:server server}
        session (api/session-create browser)]
    (assoc browser :session session)))

(def url api/go-url)
(def back api/go-back)
(def fwd api/go-fwd)
(def end api/session-delete)

(def get-title api/get-title)

(defn fill-in [browser selector text]
  (let [element (api/element-find browser selector)]
    (api/element-value browser element text)))

;; (defn title-matches [browser re-title]
;;   (re-matches re-title (api/get-title browser)))

;; (defmacro is-title-matches [browser re-title]
;;   `(let [title# (api/get-title ~browser)]
;;      (re-matches ~re-title title#)))

;; (def is-title-matches (comp title-matches (fn [x] (is x))))

(defn wait [sec]
  (Thread/sleep (* sec 1000)))

(deftest foo
  (let [browser (make-browser "127.0.0.1" 4444)]
    (url browser "http://ya.ru")
    (let [title (get-title browser)]
      (is (= title "Яндекс")))
    (wait 6)
    (fill-in browser "//input[@id=\"text\"]" "Clojure official site\uE007")

    ;; (end browser)
    ))

;; (is-title-matches #"Яндекс2")
;; (-> (title-matches #"Яндекс2") is)

;; (defmacro with-server [host port & body]
;;   `(let [server# (make-server ~host ~port)
;;          session# (api/session-create server#)]
;;      (with-bindings {#'*server* server#
;;                      #'*session* session#}
;;        (let [result# (do ~@body)]
;;          (api/session-delete server# session#)
;;          result#))))

;; (with-server "127.0.0.1" 4444 (url "http://ya.ru") (back))
