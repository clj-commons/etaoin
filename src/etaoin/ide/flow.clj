(ns etaoin.ide.flow
  (:require [cheshire.core :refer [parse-string]]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [etaoin.api :refer :all]
            [etaoin.ide.api :refer [run-command-with-log str->var]]
            [etaoin.ide.spec :as spec]))

(declare execute-commands)

(defn execute-branch
  [driver {:keys [this branch]} opt]
  (when (run-command-with-log driver this opt)
    (execute-commands driver branch opt)
    true))

(defn execute-if
  [driver {:keys [if else-if else end]} opt]
  (or (execute-branch driver if opt)
      (some #(execute-branch driver % opt) else-if)
      (execute-commands driver (:branch else) opt))
  (run-command-with-log driver end opt))

(defn execute-times
  [driver {:keys [this branch end]} opt]
  (let [n (run-command-with-log driver this opt)]
    (doseq [commands (repeat n branch)]
      (execute-commands driver commands opt))
    (run-command-with-log driver end opt)))

(defn execute-do
  [driver {:keys [this branch repeat-if]} opt]
  (run-command-with-log driver this opt)
  (loop [commands branch]
    (execute-commands driver commands opt)
    (when (run-command-with-log driver repeat-if opt)
      (recur commands))))

(defn execute-while
  [driver {:keys [this branch end]} opt]
  (while (run-command-with-log driver this opt)
    (execute-commands driver branch opt))
  (run-command-with-log driver end opt))

(defn execute-for-each
  [driver {:keys [this branch end]} {vars :vars :as opt}]
  (let [[var-name arr] (run-command-with-log driver this opt)]
    (doseq [val arr]
      (swap! vars assoc var-name val)
      (execute-commands driver branch opt))
    (run-command-with-log driver end opt)))

(defn execute-cmd-with-open-window
  [driver {:keys [windowHandleName windowTimeout] :as cmd} {vars :vars :as opt}]
  (let [init-handles  (set (get-window-handles driver))
        _             (run-command-with-log driver cmd opt)
        _             (wait (/ windowTimeout 1000))
        final-handles (set (get-window-handles driver))
        handle        (first (clojure.set/difference final-handles init-handles))]
    (swap! vars assoc (str->var windowHandleName) handle)))

(defn execute-commands
  [driver commands opt]
  (doseq [[cmd-name cmd] commands]
    (case cmd-name
      :if                   (execute-if driver cmd opt)
      :times                (execute-times driver cmd opt)
      :do                   (execute-do driver cmd opt)
      :while                (execute-while driver cmd opt)
      :for-each             (execute-for-each driver cmd opt)
      :cmd-with-open-window (execute-cmd-with-open-window driver cmd opt)
      :cmd                  (run-command-with-log driver cmd opt)
      (throw (ex-info "Command is not valid" {:command cmd})))))

(defn run-ide-test
  [driver {:keys [commands]} & [opt]]
  (let [command->kw   (fn [{:keys [command] :as cmd}]
                        (assoc cmd :command (keyword command)))
        commands      (map command->kw commands)
        commands-tree (s/conform ::spec/commands commands)]
    (when (s/invalid?  commands-tree)
      (throw (ex-info "Incomplete or invalid command in the config"
                      {:explain-data (s/explain-data ::spec/commands commands)})))
    (execute-commands driver commands-tree opt)))

(defn get-tests-by-suite-id
  [suite-id id {:keys [suites tests]}]
  (let [test-ids    (-> (filter #(= suite-id (id %)) suites)
                        first
                        :tests
                        set)
        suite-tests (filter #(test-ids (:id %)) tests)]
    suite-tests))

(defn find-tests
  [{:keys [test-id test-ids suite-id suite-ids test-name suite-name test-names suite-names]}
   {:keys [tests] :as parsed-file}]
  (let [test-ids    (cond-> #{}
                      test-id     (conj (first (filter #(= test-id (:id %)) tests)))
                      test-name   (conj (first (filter #(= test-name (:name %)) tests)))
                      suite-id    (into (get-tests-by-suite-id suite-id :id parsed-file))
                      suite-name  (into (get-tests-by-suite-id suite-name :name parsed-file))
                      test-ids    (into (filter #((set test-ids) (:id %)) tests))
                      suite-ids   (into (mapcat #(get-tests-by-suite-id % :id parsed-file) suite-ids))
                      test-names  (into (filter #((set test-names) (:name %)) tests))
                      suite-names (into (mapcat #(get-tests-by-suite-id % :name parsed-file) suite-names)))
        tests-found (filter test-ids tests)]
    (if (empty? tests-found)
      tests
      tests-found)))

(defn run-ide-script
  "Runs the received ide file

  Arguments:

  - `driver`: a driver instance

  - `source`: file path to the ide config, or file, or io/resource

  - `opt`: a map of optional parameters
  -- `:test-...` and `:suite-...` are used for selection of specific tests,
  When not passed, runs all tests from the file
  -- `:base-url` url of the main page from which tests start.
  When not passed, the base url from the file is used."

  [driver source & [opt]]
  (let [parsed-file (-> source
                        slurp
                        (parse-string true))
        opt-search  (select-keys opt [:test-name :test-id :test-ids
                                      :suite-name :suite-id :suite-ids
                                      :test-names :suite-names])
        tests-found (find-tests opt-search parsed-file)
        opt         (merge {:base-url (:url parsed-file)
                            :vars     (atom {})}
                           opt)]
    (doseq [test tests-found]
      (run-ide-test driver test opt))))
