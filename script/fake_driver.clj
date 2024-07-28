(ns fake-driver
  "A fake WebDriver to support testing, adapt as necessary for tests"
  (:require [babashka.cli :as cli]
            [cheshire.core :as json]
            [lread.status-line :as status]
            [org.httpkit.server :as server]))

(def cli-spec {:help {:desc "This usage help" :alias :h}
               :port {:ref "<port>"
                      :desc "Expose server on this port"
                      :coerce :int
                      :default 8888
                      :alias :p}
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
      (when (:start-server opts)
        (server/run-server (make-handler opts) opts))))

  @(promise))
