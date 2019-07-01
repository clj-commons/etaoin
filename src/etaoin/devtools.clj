(ns etaoin.devtools
  (:require
   [clojure.string :as str]
   [cheshire.core :as json]
   [etaoin.api :as api]))


(defn parse-json
  [string]
  (json/parse-string string true))


(defn parse-method
  [^String method]
  (let [[topname
         lowname]
        (-> (str/lower-case method)
            (str/split #"\." 2))]
    (keyword topname lowname)))


(defn process-log
  [log]

  (let [{:keys [message]} log
        message (parse-json message)
        _type (some-> message :message :method parse-method)]

    (-> log
        (merge message)
        (assoc :_type _type))))


(defn request?
  [log]
  (some-> log :_type namespace (= "network")))


(defn group-requests
  [logs]
  (group-by
   (fn [log]
     (some-> log :message :params :requestId))
   logs))



(defn log->request
  [acc log]

  (let [{:keys [_type message]} log
        {:keys [params]} message]

    (case _type

      :network/requestwillbesent
      (let [{:keys [request requestId type]} params
            {:keys [method headers url hasPostData]} request
            type (some-> type str/lower-case keyword)
            xhr? (identical? type :xhr)]
        (-> acc
            (assoc :id requestId)
            (assoc :type type)
            (assoc :xhr? xhr?)
            (assoc :state 1)
            (assoc :url url)
            (assoc :with-data? hasPostData)
            (assoc :request
                   {:method (some-> method str/lower-case keyword)
                    :headers headers})))

      :network/responsereceived
      (let [{:keys [response]} params
            {:keys [method headers mimeType remoteIPAddress]} response
            {:keys [status]} headers]
        (-> acc
            (assoc :state 2)
            (assoc :response
                   {:status status
                    :headers headers
                    :mime mimeType
                    :remote-ip remoteIPAddress})))

      :network/loadingfinished
      (-> acc
          (assoc :state 4)
          (assoc :done? true))

      :network/loadingfailed
      (-> acc
          (assoc :failed? true))

      ;; default
      acc)))


(defn build-request
  [logs]
  (reduce log->request {} logs))


(defn logs->requests
  [logs]
  (->> logs
       (filter request?)
       group-requests
       vals
       (mapv build-request)))


(defn ajax?
  [request]
  (:xhr? request))


(defn logs->ajax
  [logs]
  (->> logs
       logs->requests
       (filterv ajax?)))


(defn request-done?
  [request]
  (:done? request))


(defn request-failed?
  [request]
  (:failed? request))


(defn request-success?
  [request]
  (every-pred request-done? (complement request-failed?)))


;;
;; API
;;


(defn get-performance-logs
  [driver]
  (->> (api/get-logs* driver "performance")
       (mapv (comp process-log api/process-log))))


(defn get-requests
  [driver]
  (-> driver
      get-performance-logs
      logs->requests))


(defn get-ajax
  [driver]
  (-> driver
      get-performance-logs
      logs->ajax))
