(ns webdriver.client
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [slingshot.slingshot :refer [try+ throw+]]))

;;
;; defaults
;;

(def ^:dynamic *default-api-params*
  {:as :json
   :accept :json
   :content-type :json
   :form-params {}
   :throw-exceptions false
   :debug false})

(def default-pool-params
  {:timeout 5
   :threads 4
   :insecure? false
   :default-per-route 10})

;;
;; helpers
;;


(defn url-item-str [item]
  (cond
    (keyword? item) (name item)
    (symbol? item) (name item)
    (string? item) item
    :else (str item)))

(defn get-url-path [items]
  (str/join "/" (map url-item-str items)))

(defn status-selector [resp]
  (-> resp :status integer?))

;;
;; client
;;

(defmacro with-pool [opt & body]
  `(client/with-connection-pool
     (merge ~default-pool-params ~opt)
     ~@body))

(defmacro with-params [opt & body]
  `(binding [*default-api-params* (merge *default-api-params* ~opt)]
     ~@body))

(defn call
  ([server method path-args]
   (call server method path-args {}))
  ([server method path-args payload]
   (let [path (get-url-path path-args)
         url (-> server :url (str "/" path))
         params (merge *default-api-params*
                       {:url url
                        :method method
                        :form-params payload})]
     (let [resp (client/request params)]
       (if (-> resp :status (= 200))
         (:body resp)
         (throw+ {:type ::http-error
                  :status (:status resp)
                  :response (-> resp :body (parse-string true))
                  :server server
                  :method method
                  :path path
                  :payload payload}))))))
