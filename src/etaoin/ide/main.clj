(ns etaoin.ide.main
  "
  Provide an CLI entry point for running IDE files.
  Example:

  ```shell
  clojure -M -m etaoin.ide.main -d firefox -p '{:port 8888}' -f /path/to/script.side
  ```

  See the [User Guide](/doc/01-user-guide.adoc#selenium-ide-cli) for more info.
  "
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [etaoin.api :as api]
   [etaoin.ide.flow :as flow]
   [etaoin.impl.util :as util]))

(set! *warn-on-reflection* true)

(def ^:private browsers-set
  #{:chrome :safari :firefox :edge :phantom})

(defn- str->vec
  [string]
  (str/split string #","))

(def ^:private cli-options
  [["-d" "--driver-name name" "The name of driver. The default is `chrome`"
    :default :chrome
    :parse-fn keyword
    :validate [browsers-set
               (str "Must be one of the list items - " (str/join ", " (map name browsers-set)))]]

   ["-p" "--params params" "Parameters for the driver represented as an EDN string, e.g. '{:port 8080}'"
    :default {}
    :parse-fn read-string]

   ["-f" "--file path" "Path to an IDE file on disk"]

   ["-r" "--resource path" "Path to an IDE resource"]

   [nil "--test-ids ids" "Comma-separeted test ID(s)"
    :parse-fn str->vec]

   [nil "--suite-ids ids" "Comma-separeted suite ID(s)"
    :parse-fn str->vec]

   [nil "--test-names names" "Comma-separeted test name(s)"
    :parse-fn str->vec]

   [nil "--suite-names names" "Comma-separeted suite name(s)"
    :parse-fn str->vec]

   [nil "--base-url url" "Base URL for tests"]

   ["-h" "--help"]])

(def ^:private help
  "
This is a CLI interface for running Selenium IDE files.

Usage examples:

;; from clojure
clojure -M -m etaoin.ide.main -d firefox -p '{:port 8888}' -r ide/test.side

;; from a jar
java -cp .../poject.jar -m etaoin.ide.main -d firefox -p '{:port 8888}' -f ide/test.side

Options:")

(defn ^:private usage [options-summary]
  (->> [help options-summary]
       (str/join \newline)))

(defn ^:private error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(def ^:private opt-fields
  [:base-url
   :test-ids
   :test-names
   :suite-ids
   :suite-names])

(defn- run-script
  "
  Run a Selenium IDE file. The `source` is something
  that might be `slurp`ed.
  "
  [source {:keys [driver-name params] :as options}]

  (let [opt (select-keys options opt-fields)]
    (api/with-driver driver-name params driver
      (flow/run-ide-script driver source opt))))

(defn -main
  "The main CLI entrypoint.

  See [Selenium IDE CLI docs](/doc/01-user-guide.adoc#selenium-ide-cli)"
  [& args]
  (let [{:keys [errors summary options]}
        (cli/parse-opts args cli-options)

        {:keys [help file resource]}
        options]

    (cond

      errors
      (util/exit 1 (error-msg errors))

      help
      (util/exit 0 (usage summary))

      file
      (let [ide-file (io/file file)]
        (when-not (and (.exists ide-file)
                       (not (.isDirectory ide-file)))
          (util/exit 1 "The IDE file not found"))
        (run-script ide-file options))

      resource
      (if-let [r (io/resource resource)]
        (run-script r options)
        (util/exit 1 "Resource not found"))

      :else
      (util/exit 1 "Specify the path to the ide file: `--file` or `--resource`"))))
