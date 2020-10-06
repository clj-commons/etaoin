(ns etaoin.ide.main
  (:require [clojure.tools.cli :refer [parse-opts summarize]]
            [etaoin.api :refer :all]
            [etaoin.ide.flow :refer [run-ide-script]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [etaoin.ide.flow :as ide])
  (:gen-class))

(defn str->vec
  [string]
  (str/split string #","))

(def cli-options
  [["-d" "--driver-name name" "The name of driver. The default is `chrome`"
    :default :chrome
    :parse-fn keyword
    :validate [#(#{:chrome :safari :firefox :edge :phantom} %)
               "Must be one of the list items - chrome, safari, firefox, egde, phantom"]]

   ["-p" "--params params" "Parameters for the driver represented as edn string"
    :default {}
    :parse-fn read-string]

   ["-f" "--file path" "Path to the ide file on disk"
    :validate [#(let [file (io/file %)]
                  (and (.exists file)
                       (not (.isDirectory file)))) "The IDE file not found"]]

   ["-r" "--resource path" "Path to the resource"
    :parse-fn #(-> % io/resource str)
    :validate [#(not (str/blank? %)) "Resource not found"]]

   [nil "--test-ids" "Comma-separeted test ID(s)"
    :parse-fn str->vec]

   [nil "--suite-ids" "Comma-separeted suite ID(s)"
    :parse-fn str->vec]

   [nil "--test-name" "Comma-separeted test name(s)"
    :parse-fn str->vec]

   [nil "--suite-name" "Comma-separeted suite name(s)"
    :parse-fn str->vec]

   [nil "--base-url" "Base url for test"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["This is cli interface for running ide files."
        ""
        "Usage:"
        "Required parameter is the path to the ide file. `--file` or `--resource`"
        ""
        "lein run -m etaoin.ide.main -d firefox -p '{:port 8888 :args [\"--no-sandbox\"]} -r ide/test.side'"
        ""
        "java -cp .../poject.jar -m etaoin.ide.main -d firefox -p '{:port 8888} -f ide/test.side'"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (or (:file options)
          (:resource options))
      {:options options}

      :else
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [{:keys [driver-name params file resource]} options
            opt (select-keys options [:base-url :test-ids
                                      :test-names :suite-ids
                                      :suite-names])
            file-path (or file
                          resource)]
        (with-driver driver-name params driver
          (run-ide-script driver file-path opt))))))
