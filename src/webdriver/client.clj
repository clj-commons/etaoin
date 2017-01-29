(ns webdriver.client
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [slingshot.slingshot :refer [throw+]]))

;;
;; defaults
;;

(def ^:dynamic *default-api-params*
  {:as :json
   :accept :json
   :content-type :json
   :form-params {}
   :debug true})

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
                        :form-params payload
                        :throw-exceptions false})
         resp (client/request params)
         body (:body resp)
         error (delay {:type :webdriver/http-error
                       :status (:status resp)
                       :response (if (string? body)
                                   (try
                                     (parse-string (str/replace body #"Invalid Command Method -" "") true)
                                     (catch Throwable _ body))
                                   body)
                       :server server
                       :method method
                       :path path
                       :payload payload})]
     (cond
         (-> resp :status (not= 200))
         (throw+ @error)

         (-> body :status (or 0) (> 0))
         (throw+ @error)

         :else
         body))))
