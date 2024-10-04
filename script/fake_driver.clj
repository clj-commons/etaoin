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
      ;; Get Status
      (and (= :get request-method) (= "/status" uri))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:value {:ready true
                                            :message "I'm ready"}})}
      ;; Create Session
      (and (= :post request-method) (= "/session" uri))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:value {:sessionId (random-uuid)}})}
      ;; Get Window Handle
      (and (= :get request-method) (re-matches #"/session/[^/]+/window" uri))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:value (random-uuid)})}
      ;; Fake Driver is... well... fake.
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
