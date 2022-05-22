(ns test
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as string]
            [doric.core :as doric]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- test-def [os id browser]
  {:os os
   :cmd (->> ["bb test" id
              (when browser (str "--browser=" browser))
              (when (= "ubuntu" os) "--launch-virtual-display")]
             (remove nil?)
             (string/join " "))
   :desc (->> [id os browser]
              (remove nil?)
              (string/join " "))})

(defn- github-actions-matrix []
  (let [oses ["macos" "ubuntu" "windows"]
        ide-browsers ["chrome" "firefox"]
        api-browsers ["chrome" "firefox" "edge" "safari"]]
    (->> (concat
          (for [os oses]
            (test-def os "unit" nil))
          (for [os oses
                browser ide-browsers]
            (test-def os "ide" browser))
          (for [os oses
                browser api-browsers
                :when (not (or (and (= "ubuntu" os) (some #{browser} ["edge" "safari"]))
                               (and (= "windows" os) (= "safari" browser))))]
            (test-def os "api" browser)))
         (sort-by :desc)
         (into []))))

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

(def args-usage "Valid args:
  (api|ide) [--browser=<edge|safari|firefox|chrome>]... [--launch-virtual-display]
  (unit|all) [--launch-virtual-display]
  matrix-for-ci [--format=json]
  --help

Commands:
  unit           Run only unit tests
  api            Run only api tests, optionally specifying browsers to override defaults
  ide            Run only ide tests, optionally specifying browsers to override defaults
  all            Run all tests using browser defaults
  matrix-for-ci  Return text matrix for GitHub Actions

Options:
  --launch-virtual-display   Launch a virtual display for browsers (use on linux only)
  --help                     Show this help

Notes:
- ide tests default to firefox and chrome only.
- api tests default browsers based on OS on which they are run.
- launching a virtual display is necessary for GitHub Actions but not so for CircleCI")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (get opts "matrix-for-ci")
      (if (= "json" (get opts "--format"))
        (status/line :detail (-> (github-actions-matrix)
                                 (json/generate-string #_{:pretty true})))
        (status/line :detail (->> (github-actions-matrix)
                                  (doric/table [:os :cmd :desc]))))

      :else
      (let [clojure-args (cond
                           (get opts "api") "-M:test --namespace etaoin.api-test"
                           (get opts "ide") "-M:test --namespace etaoin.ide-test"
                           (get opts "unit") "-M:test --namespace-regex '.*unit.*-test$'"
                           :else "-M:test")
            browsers (->> (get opts "--browser") (keep identity))
            env (cond-> {}
                  (seq browsers)
                  (assoc (if (get opts "api")
                           "ETAOIN_TEST_DRIVERS"
                           "ETAOIN_IDE_TEST_DRIVERS")
                         (mapv keyword browsers))

                  (get opts "--launch-virtual-display")
                  (assoc "DISPLAY" ":99.0"))
            shell-opts (if (seq env)
                         {:extra-env env}
                         {})]
        (when (get opts "--launch-virtual-display")
          (status/line :head "Launching virtual display")
          (launch-xvfb))
        (status/line :head "Running tests")
        (shell/clojure shell-opts clojure-args)))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))

