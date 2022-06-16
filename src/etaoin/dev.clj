(ns etaoin.dev
  "Chrome devtools features"
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [etaoin.api :as api]))

(defn- try-parse-int
  [line]
  (try (Integer/parseInt line)
       (catch Exception _e
         line)))

(defn- parse-json
  [string]
  (json/parse-string string true))

(defn- parse-method
  "Turns a string like 'Network.SomeAction' into a keyword :network/someaction."
  [^String method]
  (let [[topname
         lowname]
        (-> (str/lower-case method)
            (str/split #"\." 2))]
    (keyword topname lowname)))

(defn- process-log
  "Takes a log map, parses its message and merges the message into the map."
  [log]
  (let [{:keys [message]} log
        message           (parse-json message)
        _type             (some-> message :message :method parse-method)]

    (-> log
        (merge message)
        (assoc :_type _type))))

(defn- request?
  "Return true if `log` entry belongs to a network domain."
  [log]
  (some-> log :_type namespace (= "network")))

(defn- group-requests
  "Group a set of request `logs` by their ID."
  [logs]
  (group-by
    (fn [log]
      (some-> log :message :params :requestId))
    logs))

(defn- log->request
  "A helper for a further reduce (see below).
  Acc is an accumulation map."
  [acc log]

  (let [{:keys [_type message]} log
        {:keys [params]}        message]

    (case _type

      :network/requestwillbesent
      (let [{:keys [request
                    requestId
                    type]}        params
            {:keys [method
                    headers
                    url
                    hasPostData]} request
            type                  (some-> type str/lower-case keyword)
            xhr?                  (identical? type :xhr)]
        (assoc acc
               :state 1
               :id requestId
               :type type
               :xhr? xhr?
               :url url
               :with-data? hasPostData
               :request {:method  (some-> method str/lower-case keyword)
                         :headers headers}))

      :network/responsereceived
      (let [{:keys [response]}                         params
            {:keys [headers mimeType remoteIPAddress]} response
            {:keys [status]}                           headers]
        (assoc acc
               :state 2
               :response {:status    (try-parse-int status)
                          :headers   headers
                          :mime      mimeType
                          :remote-ip remoteIPAddress}))

      :network/loadingfinished
      (assoc acc
             :state 4
             :done? true)

      :network/loadingfailed
      (assoc acc
             :failed? true)

      ;; default
      acc)))

(defn- build-request
  "Takes a vector of request logs of the same ID and build a request map."
  [logs]
  (reduce log->request {} logs))

(defn logs->requests
  "Return list of log entries `logs` converted to requests."
  [logs]
  (->> logs
       (filter request?)
       group-requests
       vals
       (mapv build-request)))

(defn ajax?
  "Return true when `request` is XHR."
  [request]
  (:xhr? request))

(defn logs->ajax
  "The same as [[logs->requests]] but returns only AJAX requests."
  [logs]
  (->> logs
       logs->requests
       (filterv ajax?)))

(defn request-done?
  "Return true when `request` has concluded.
  Completion does not indicate success. See [[request-success?]], [[request-failed?]]"
  [request]
  (:done? request))

(defn request-failed?
  "Return true when `request` has failed. Does not indicate completion, see [[request-done?]]"
  [request]
  (:failed? request))

(def request-success?
  "Return true when `request` has completed and not failed."
  (every-pred request-done? (complement request-failed?)))

(defn get-performance-logs
  "Have `driver` return a seq of special performance logs from the dev console.

  Works only when `perfLoggingPrefs` is enabled, see [DevTools](/doc/01-user-guide.adoc#devtools)."
  [driver]
  (->> (api/get-logs driver "performance")
       (mapv (comp process-log api/process-log))))

(defn get-requests
  "Have `driver` return a list of HTTP requests made by the browser.

  Works only when `perfLoggingPrefs` is enabled, see [DevTools](/doc/01-user-guide.adoc#devtools)."
  [driver]
  (-> driver
      get-performance-logs
      logs->requests))

(defn get-ajax
  "Have `driver` return a list of XHR (Ajax) HTTP requests made by the browser.

  Works only when `perfLoggingPrefs` is enabled, see [DevTools](/doc/01-user-guide.adoc#devtools)."
  [driver]
  (-> driver
      get-performance-logs
      logs->ajax))
