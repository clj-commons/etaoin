(ns test
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as string]
            [doric.core :as doric]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- test-def [os id platform browser]
  {:os os
   :cmd (->> ["bb test" id
              (str "--platform=" platform)
              (when browser (str "--browser=" browser))
              (when (= "ubuntu" os) "--launch-virtual-display")]
             (remove nil?)
             (string/join " "))
   :desc (->> [id os browser platform]
              (remove nil?)
              (string/join " "))})

(defn- github-actions-matrix []
  (let [oses ["macos" "ubuntu" "windows"]
        ide-browsers ["chrome" "firefox"]
        api-browsers ["chrome" "firefox" "edge" "safari"]
        platforms ["jvm" "bb"]]
    (->> (concat
          (for [os oses
                platform platforms]
            (test-def os "unit" platform nil))
          (for [os oses
                platform platforms
                browser ide-browsers]
            (test-def os "ide" platform browser))
          (for [os oses
                platform platforms
                browser api-browsers
                :when (not (or (and (= "ubuntu" os) (some #{browser} ["edge" "safari"]))
                               (and (= "windows" os) (= "safari" browser))))]
            (test-def os "api" platform browser)))
         (sort-by :desc)
         (into [{:os "ubuntu" :cmd "bb lint" :desc "lint"}
                {:os "macos" :cmd "bb test-doc" :desc "test-doc"}]))))

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

(defn- running-in-docker? []
  (fs/exists? "/.dockerenv"))

(def valid-browsers ["chrome" "firefox" "edge" "safari"])
(def valid-platforms ["jvm" "bb"])

(defn valid-opts [opts]
  (format "<%s>" (string/join "|" opts)))

(def args-usage (-> "Valid args:
  (api|ide) [--browser=BROWSER]... [--launch-virtual-display] [--platform=PLATFORM]
  (unit|all) [--launch-virtual-display] [--platform=PLATFORM]
  matrix-for-ci [--format=json]
  --help

Commands:
  unit           Run unit tests
  api            Run api tests
  ide            Run ide tests
  all            Run all tests using browser defaults
  matrix-for-ci  Return text matrix for GitHub Actions

Options:
  --browser=BROWSER          {{valid-browsers}} overrides defaults
  --platform=PLATFORM        {{valid-platforms}} [default: jvm]
  --launch-virtual-display   Launch a virtual display for browsers
  --help                     Show this help

Notes:
- ide tests default to firefox and chrome only.
- api tests default browsers based on OS on which they are run.
- launching a virtual display is automatic when in docker"
                    (string/replace "{{valid-browsers}}" (valid-opts valid-browsers))
                    (string/replace "{{valid-platforms}}" (valid-opts valid-platforms))))

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [browsers (->> (get opts "--browser") (keep identity))
          platform (get opts "--platform")]
      (when (or (not-every? (set valid-browsers) browsers)
                (and platform (not (some #{platform} valid-platforms))))
        (status/die 1 args-usage))
      (cond
        (get opts "matrix-for-ci")
        (if (= "json" (get opts "--format"))
          (status/line :detail (-> (github-actions-matrix)
                                   (json/generate-string #_{:pretty true})))
          (status/line :detail (->> (github-actions-matrix)
                                    (doric/table [:os :cmd :desc]))))

        :else
        (let [virtual-display? (or (get opts "--launch-virtual-display")
                                   (running-in-docker?))
              env (cond-> {}
                    (seq browsers)
                    (assoc (if (get opts "api")
                             "ETAOIN_TEST_DRIVERS"
                             "ETAOIN_IDE_TEST_DRIVERS")
                           (mapv keyword browsers))

                    virtual-display?
                    (assoc "DISPLAY" ":99.0"))
              shell-opts (if (seq env)
                           {:extra-env env}
                           {})
              test-id (cond
                        (get opts "api") "api"
                        (get opts "ide") "ide"
                        (get opts "unit") "unit"
                        :else "all")
              cp-args (case platform
                        "jvm" ["-M:test"]
                        "bb" (let [aliases (cond-> ":script:bb-test:test"
                                             (not= "api" test-id) (str ":bb-spec"))]
                               ["--classpath"
                                (with-out-str (shell/clojure "-Spath" (str "-A" aliases)))
                                "--main" "bb-test-runner"]))
              test-runner-args (case test-id
                                 "api" ["--namespace-regex" "etaoin.api.*-test$"]
                                 "ide" ["--namespace" "etaoin.ide-test"]
                                 "unit" ["--namespace-regex" ".*unit.*-test$"]
                                 "all" [])
              test-cmd-args (concat cp-args test-runner-args)]
          (when virtual-display?
            (status/line :head "Launching virtual display")
            (launch-xvfb))
          (status/line :head "Running %s tests on %s%s"
                       test-id
                       platform
                       (if (seq browsers)
                         (str " against browsers: " (string/join ", " browsers))
                         ""))
          (case platform
            "jvm" (apply shell/clojure shell-opts test-cmd-args)
            "bb" (apply shell/command shell-opts "bb" test-cmd-args)))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))

