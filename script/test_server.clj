(ns test-server
  (:require [babashka.cli :as cli]
            [babashka.http-server :as server]
            [lread.status-line :as status]))

(def cli-spec {:help {:desc "This usage help" :alias :h}
               :port {:ref "<port>"
                      :desc "Expose server on this port"
                      :coerce :int
                      :default 8888
                      :alias :p}
               :dir {:ref "<dir>"
                     :desc "Serve static assets from this dir"
                     :default "./env/test/resources/static"
                     :alias :d}})

(defn- usage-help []
  (status/line :head "Usage help")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:port :dir :help]})) )

(defn- usage-fail [msg]
  (status/line :error msg)
  (usage-help)
  (System/exit 1))

(defn -main [& args]
  (let [opts (cli/parse-opts args {:spec cli-spec
                                   :restrict true
                                   :error-fn (fn [{:keys [msg]}]
                                               (usage-fail msg))})]
    (if (:help opts)
      (usage-help)
      (do
        (status/line :detail "Test server static dir: %s" (:dir opts))
        (server/exec opts)))))
