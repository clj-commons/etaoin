(ns fake-driver
  "A fake WebDriver to support testing, adapt as necessary for tests"
  (:require [babashka.cli :as cli]
            [cheshire.core :as json]
            [lread.status-line :as status]
            [org.httpkit.server :as server])
  (:import [sun.misc Signal SignalHandler]))

(def cli-spec {:help {:desc "This usage help" :alias :h}
               :port {:ref "<port>"
                      :desc "Expose server on this port"
                      :coerce :int
                      :default 8888
                      :alias :p}
               :sigterm-filename {:ref "<filename>"
                                  :desc "Log a line to this filename when quit via sig term"}
               :start-server {:ref "<boolean>"
                              :desc "Start a fake webdriver request handler"
                              :default true}})

(defn- usage-help []
  (status/line :head "Usage help")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:port :sigterm-filename :start-server :help]})) )

(defn- usage-fail [msg]
  (status/line :error msg)
  (usage-help)
  (System/exit 1))

(defn signal-handler [signal-id handler-fn]
  (let [signal (Signal. signal-id)
        handler (reify SignalHandler
                  (handle [_ _] (handler-fn)))]
    (Signal/handle signal handler)))

(defn make-handler [_opts]
  (fn handle-request [{:keys [request-method uri] :as _req}]
    (cond
      (and (= :post request-method) (= "/session" uri))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:sessionId (random-uuid)})}
      (and (= :get request-method) (= "/status" uri))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:ready true})}
      :else
      {:status 404})))

(defn -main [& args]
  (let [opts (cli/parse-opts args {:spec cli-spec
                                   :restrict true
                                   :error-fn (fn [{:keys [msg]}]
                                               (usage-fail msg))})]
    (if (:help opts)
      (usage-help)
      (do
        (when-let [log (:sigterm-filename opts)]
          (signal-handler "TERM" (fn sigterm-handler []
                                   ;; I don't think we need to worry about concurrency for our use case
                                   (spit log "SIGTERM received, quitting" :append true)
                                   (System/exit 0))))

        (when (:start-server opts)
          (server/run-server (make-handler opts) opts)))))

  @(promise))
