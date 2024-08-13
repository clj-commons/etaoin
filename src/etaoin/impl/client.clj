(ns ^:no-doc etaoin.impl.client
  (:require
   [babashka.http-client :as client]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [etaoin.impl.proc :as proc]
   [etaoin.impl.util :as util]
   [slingshot.slingshot :refer [throw+]]))

(set! *warn-on-reflection* true)

;;
;; defaults
;;

(def default-timeout
  "HTTP timeout in seconds. The current value may seem high,
  but according to my experience with SPA application full of React
  modules even 20 seconds can insufficient time for a driver to process
  your request."
  60)

(defn read-timeout []
  (if-let [t (System/getenv "ETAOIN_TIMEOUT")]
    (Integer/parseInt t)
    default-timeout))

(def timeout (read-timeout))

(def default-api-params
  {:headers {:accept "application/json"
             :content-type "application/json"}
   :timeout (* 1000 timeout)  ;; request timeout
   :connect-timeout (* 1000 timeout)})

;;
;; helpers
;;

(defn- url-item-str [item]
  (cond
    (keyword? item) (name item)
    (symbol? item)  (name item)
    (string? item)  item
    :else           (str item)))

(defn- get-url-path [items]
  (str/join "/" (map url-item-str items)))

(defn- parse-json [body]
  (let [body* (str/replace body #"Invalid Command Method -" "")]
    (try
      (json/parse-string body* true)
      (catch Throwable _ body))))

(defn- error-response [body]
  (if (string? body)
    (parse-json body)
    body))

(defn- realized-driver
  "Realize process liveness (or actually deadness, if dead)"
  [{:keys [process] :as driver}]
  (try
    (if (and process (not (proc/alive? process)))
      (assoc driver :process (proc/result process))
      driver)
    (catch Throwable ex
      ;; if, by chance, something goes wrong while trying to realize process liveness
      (assoc driver :process-liveness-ex ex))))

(defn http-request
  "an isolated http-request to support mocking"
  [params]
  (client/request params))

;;
;; client
;;

(defn call
  [{driver-type :type :keys [host port webdriver-url] :as driver}
   method path-args payload]
  (let [path   (get-url-path path-args)
        url    (if webdriver-url
                 (format "%s/%s" webdriver-url path)
                 (format "http://%s:%s/%s" host port path))
        params (cond-> (merge
                        default-api-params
                        {:uri     url
                         :method  method
                         :throw   false})
                 (= :post method)
                 (assoc :body (.getBytes (json/generate-string (or payload {}))
                                         "UTF-8")))
        _ (log/debugf "%s %s %6s %s %s"
                      (name driver-type)
                      (if webdriver-url
                        (util/strip-url-creds webdriver-url)
                        (str host ":" port))
                      (-> method name str/upper-case)
                      path
                      (-> payload (or "")))
        error (delay {:type     :etaoin/http-ex
                      :driver   (realized-driver driver)
                      :webdriver-url webdriver-url
                      :host     host
                      :port     port
                      :method   method
                      :path     path
                      :payload  payload})
        resp  (try (http-request params)
                   (catch Throwable ex
                     {:exception ex}))]
    (if (:exception resp)
      (throw+ @error (:exception resp))
      (let [body  (some-> resp :body parse-json)
            error (delay (assoc @error
                                :type :etaoin/http-error
                                :status (:status resp)
                                :response (error-response body)))]
        (cond
          (-> resp :status (not= 200))
          (throw+ @error)

          (-> body :status (or 0) (> 0))
          (throw+ @error)

          :else
          body)))))

(comment
  (http-request {:method :get :uri "https://clojure.org"})

  )
