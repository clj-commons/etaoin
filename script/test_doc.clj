(ns test-doc
  (:require [babashka.cli :as cli]
            [helper.os :as os]
            [helper.shell :as shell]
            [helper.virtual-display :as virtual-display]
            [lread.status-line :as status]))

(defn generate-doc-tests []
  (status/line :head "Generating tests for code blocks in documents")
  (shell/clojure "-X:test-doc-blocks gen-tests"))

(defn run-clj-doc-tests [{:keys [shell-opts test-runner-args]}]
  (status/line :head "Running code block tests under Clojure")
  (apply shell/clojure shell-opts "-M:test:test-docs" test-runner-args))

(def cli-spec {:help {:desc "This usage help" :alias :h}
               :launch-virtual-display {:desc "Launch virtual display support for browsers (linux)"
                                        :alias :l}
               ;; cognitect test runner pass-thru opts
               :nses {:ref "<symbols>"
                      :coerce [:symbol]
                      :alias :n
                      :desc "Symbol(s) indicating a specific namespace to test."}
               :patterns {:ref "<regex strings>"
                          :coerce [:string]
                          :alias :p
                          :desc "Regex(es) for namespaces to test."}
               :vars {:ref "<symbols>"
                      :coerce [:symbol]
                      :alias :v
                      :desc "Symbol(s) indicating fully qualified name of a specific test"}})

(defn- usage-help[]
  (status/line :head "Usage help")
  (status/line :detail "Run doc block tests")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:launch-virtual-display :help]}))
  (status/line :detail "\nCognitect test-runner args are also supported for generated tests:")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:nses :patterns :vars]}))

  (status/line :detail "\nNotes
- launching a virtual display is automatic when in docker
- be aware that doc tests often depend on other doc tests"))

(defn- usage-fail [msg]
  (status/line :error msg)
  (usage-help)
  (System/exit 1))

(defn- parse-opts [args]
  (let [opts (cli/parse-opts args {:spec cli-spec
                                   :restrict true
                                   :error-fn (fn [{:keys [msg]}]
                                               (usage-fail msg))})]
    (when-let [extra-gunk (-> (meta opts) :org.babashka/cli)]
      (usage-fail (str "unrecognized on the command line: " (pr-str extra-gunk))))
    opts))

(defn- opts->args [opts]
  (->> (select-keys opts [:nses :patterns :vars :dirs])
       (reduce (fn [acc [k v]]
                 (apply conj acc k v))
               [])))

(defn- prep [args]
  (let [opts (parse-opts args)]
    (if (:help opts)
      (usage-help)
      (let [virtual-display? (or (:launch-virtual-display opts)
                                 (os/running-in-docker?) )
            shell-opts (if virtual-display?
                         {:extra-env (virtual-display/extra-env)}
                         {})
            test-runner-args (opts->args opts)]

        (when virtual-display?
          (virtual-display/launch))

        {:shell-opts shell-opts :test-runner-args test-runner-args}))))

;; entry points

(defn test-doc [& args]
  (when-let [test-run-args (prep args)]
    (generate-doc-tests)
    (run-clj-doc-tests test-run-args))
  nil)
