(ns etaoin.ide.main
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [etaoin.api :as api]
            [etaoin.ide.flow :as flow]
            [etaoin.util :refer [exit]])
    (:gen-class))

(def browsers-set
  #{:chrome :safari :firefox :edge :phantom})

(defn str->vec
  [string]
  (str/split string #","))

(def cli-options
  [["-d" "--driver-name name" "The name of driver. The default is `chrome`"
    :default :chrome
    :parse-fn keyword
    :validate [browsers-set
               (str "Must be one of the list items - " (str/join ", " (map name browsers-set)))]]

   ["-p" "--params params" "Parameters for the driver represented as edn string"
    :default {}
    :parse-fn read-string]

   ["-f" "--file path" "Path to the ide file on disk"]

   ["-r" "--resource path" "Path to the resource"]

   [nil "--test-ids ids" "Comma-separeted test ID(s)"
    :parse-fn str->vec]

   [nil "--suite-ids ids" "Comma-separeted suite ID(s)"
    :parse-fn str->vec]

   [nil "--test-names names" "Comma-separeted test name(s)"
    :parse-fn str->vec]

   [nil "--suite-names names" "Comma-separeted suite name(s)"
    :parse-fn str->vec]

   [nil "--base-url url" "Base url for test"]
   ["-h" "--help"]])

(def help
  "This is cli interface for running ide files.

Usage examples:

lein run -m etaoin.ide.main -d firefox -p '{:port 8888 :args [\"--no-sandbox\"]} -r ide/test.side

java -cp .../poject.jar -m etaoin.ide.main -d firefox -p '{:port 8888} -f ide/test.side

Options:")

(defn usage [options-summary]
  (->> [help options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn run-script
  [source {:keys [driver-name params] :as options}]
  (let [opt (select-keys options [:base-url :test-ids
                                  :test-names :suite-ids
                                  :suite-names])]
    (api/with-driver driver-name params driver
      (flow/run-ide-script driver source opt))))

(defn -main [& args]
  (let [{:keys [errors summary options]} (parse-opts args cli-options)
        {:keys [help file resource]}     options]
    (cond
      errors
      (exit 1 (error-msg errors))

      help
      (exit 0 (usage summary))

      file
      (let [ide-file (io/file file)]
        (when-not (and (.exists ide-file)
                       (not (.isDirectory ide-file)))
          (exit 1 "The IDE file not found"))
        (run-script ide-file options))

      resource
      (if-let [r (io/resource resource)]
        (run-script r options)
        (exit 1 "Resource not found"))

      :else
      (exit 1 "Specify the path to the ide file: `--file` or `--resource`"))))
