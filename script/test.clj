(ns test
  "Prep and launch for bb and jvm tests"
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as string]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- launch-xvfb []
  (if (fs/which "Xvfb")
    (process/process "Xvfb :99 -screen 0 1024x768x24" {:out (fs/file "/dev/null")
                                                       :err (fs/file "/dev/null")})
    (status/die 1 "Xvfb not found"))
  (let [deadline (+ (System/currentTimeMillis) 10000)]
    (loop []
      (let [{:keys [exit]} (shell/command {:out (fs/file "/dev/null")
                                           :err (fs/file "/dev/null")
                                           :continue true}
                                          "xdpyinfo -display :99")]
        (if (zero? exit)
          (status/line :detail "Xvfb process looks good.")
          (if (> (System/currentTimeMillis) deadline)
            (status/die 1 "Failed to get status from Xvfb process")
            (do
              (status/line :detail "Waiting for Xvfb process.")
              (Thread/sleep 500)
              (recur))))))))

(defn- launch-fluxbox []
  (if (fs/which "fluxbox")
    (process/process "fluxbox -display :99" {:out (fs/file "/dev/null")
                                             :err (fs/file "/dev/null")})
    (status/die 1 "fluxbox not found")))

(defn- running-in-docker? []
  (fs/exists? "/.dockerenv"))

(defn- str-coll [coll]
  (string/join ", " coll))

(def valid-browsers ["chrome" "firefox" "edge" "safari"])
(def valid-suites ["api" "ide" "unit"])

(def cli-spec {:help {:desc "This usage help" :alias :h}
               :browsers {:ref "<name>"
                          :desc (str "Browsers to test against: " (str-coll valid-browsers))
                          :coerce []
                          :alias :b
                          :validate {:pred #(every? (set valid-browsers) %)
                                     :ex-msg (fn [_m]
                                               (str "--browsers must specify from: " valid-browsers))}}
               :suites {:ref "<id>"
                        :desc (str "Suites to run: " (str-coll valid-suites))
                        :coerce []
                        :alias :s
                        :validate {:pred #(every? (set valid-suites) %)
                                   :ex-msg (fn [_m]
                                             (str "--suites must specify from: " valid-suites))}}
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
  (status/line :detail "Run tests")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:browsers :suites :help]}))
  (status/line :detail "\nCognitect test-runner args are also supported (if not specifying --suites):")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:nses :patterns :vars]}))

  (status/line :detail "\nNotes
- ide tests default to (and support) firefox and chrome only (other browsers will be ignored).
- api tests default browsers based on OS on which they are run .
- unit tests pay no attention to --browsers, but do rely on firefox and chrome.
- launching a virtual display is automatic when in docker"))

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
    (when (and (seq (:suites opts))
               (seq (select-keys opts [:nses :patterns :vars])))
      (usage-fail "--suites and cognitect test-runner opts are mutually exclusive"))

    opts))

(defn- opts->args [opts]
  (->> (select-keys opts [:nses :patterns :vars]) 
       (reduce (fn [acc [k v]]
                 (apply conj acc k v))
               [])))

(defn- prep [args]
  (let [opts (parse-opts args)]
    (if (:help opts)
      (usage-help)
      (let [browsers (:browsers opts)
            virtual-display? (or (:launch-virtual-display opts)
                                 (running-in-docker?) )
            suites (set (:suites opts))
            env (cond-> {}
                  (seq browsers)
                  (assoc "ETAOIN_TEST_DRIVERS" (mapv keyword browsers)
                         "ETAOIN_IDE_TEST_DRIVERS" (mapv keyword browsers))
                  virtual-display?
                  (assoc "DISPLAY" ":99.0"))
            shell-opts (if (seq env)
                         {:extra-env env}
                         {})
            test-runner-args (cond-> (opts->args opts)
                               (suites "api") (concat ["--patterns" "etaoin.api.*-test$"])
                               (suites "ide") (concat ["--nses" "etaoin.ide-test"])
                               (suites "unit") (concat ["--patterns" ".*unit.*-test$"])
                               :always vec)]

        (when virtual-display?
          (status/line :head "Launching virtual display")
          (launch-xvfb)
          (launch-fluxbox))

        (status/line :head "Running tests")
        (status/line :detail "suites: %s" (if (seq suites) (str-coll (sort suites)) "<none specified>"))
        (status/line :detail "browsers: %s" (if (seq browsers) (str-coll (sort browsers)) "<defaults>"))
        (status/line :detail "runner-args: %s" test-runner-args)

        {:shell-opts shell-opts :test-runner-args test-runner-args}))))

;; Entry points

(defn test-bb [& args]
  (when-let [{:keys [shell-opts test-runner-args]} (prep args)]
    (apply shell/command shell-opts "bb -test:bb" test-runner-args)))

(defn test-jvm [& args]
  (when-let [{:keys [shell-opts test-runner-args]} (prep args)]
    (apply shell/clojure shell-opts "-M:test" test-runner-args)))
