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

(defn url [url]
  (api/go-url *server* *session* url))

(defn back []
  (api/go-back *server* *session*))


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

(defmacro with-server [host port & body]
  `(let [server# (make-server ~host ~port)
         session# (api/session-create server#)]
     (with-bindings {#'*server* server#
                     #'*session* session#}
       (let [result# (do ~@body)]
         (api/session-delete server# session#)
         result#))))

;; (with-server "127.0.0.1" 4444 (url "http://ya.ru") (back))
