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


(defn net?
  [log]
  (some-> log :_type namespace (= "network")))


(defn group-net
  [logs]
  (group-by
   (fn [log]
     (some-> log :message :params :requestId))
   logs))


(defn build-net
  [logs]

  (reduce
   (fn [net log]

     (let [{:keys [_type message]} log
           {:keys [params]} message]

       (case _type

         :network/requestwillbesent
         (let [{:keys [request requestId type]} params
               {:keys [method headers url hasPostData]} request
               type (some-> type str/lower-case keyword)
               xhr? (identical? type :xhr)]
           (-> net
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
           (-> net
               (assoc :state 2)
               (assoc :response
                      {:status status
                       :headers headers
                       :mime mimeType
                       :remote-ip remoteIPAddress})))

         :network/loadingfinished
         (-> net
             (assoc :state 4)
             (assoc :done? true))

         :network/loadingfailed
         (-> net
             (assoc :failed? true))

         ;; default
         net)))

   {}
   logs))


(defn logs->network
  [logs]
  (->> logs
       (filter net?)
       group-net
       vals
       (mapv build-net)))


(defn ajax?
  [net]
  (:xhr? net))


(defn logs->ajax
  [logs]
  (->> logs
       logs->network
       (filterv ajax?)))


(defn net-done?
  [net]
  (:done? net))


(defn net-failed?
  [net]
  (:failed? net))


(defn net-success?
  [net]
  (every-pred net-done? (complement net-failed?)))


;;
;; API
;;


(defn get-performance-logs
  [driver]
  (->> (api/get-logs* driver "performance")
       (mapv (comp process-log api/process-log))))


(defn get-network
  [driver]
  (-> driver
      get-performance-logs
      logs->network))


(defn get-ajax
  [driver]
  (-> driver
      get-performance-logs
      logs->ajax))
