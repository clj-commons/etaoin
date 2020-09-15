(ns etaoin.ide
  (:require [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as io]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [clojure.string :as str])
  (:import (java.net URL)))


(defn dispatch-command
  [driver command & [opt]]
  (some-> command :command keyword))

(defmulti run-command dispatch-command)

(defmethod run-command
  :default
  [driver command & _]
  (throw (ex-info "Command not implemented"
                  {:command command})))

(defn absolute-path?
  [path]
  (-> path
      str/lower-case
      (str/starts-with? "http")))

(def special-keys
  {"${KEY_ENTER}" k/enter})

(defn make-query
  [target]
  {:css (format "[%s]" target)})

(defmethod run-command
  :open
  [driver {:keys [target]} & [{base-url :base-url}]]
  (if (absolute-path? target)
    (go driver target)
    (go driver (-> (URL. base-url)
                   (URL. target)
                   str))))

(defmethod run-command
  :setWindowSize
  [driver {:keys [target]} & [{base-url :base-url}]])


(defmethod run-command
  :type
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) value))

;; TODO apply map to special-keys or auto-replace
(defmethod run-command
  :sendKeys
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) (get special-keys value)))

(defn run-ide-test
  [driver {:keys [id commands]} & [opt]]
  (doseq [command commands]
    (run-command driver command opt)))

(defn run-ide-suite
  [driver {test-ids :tests} tests & [opt]]
  (let [tests (filter #((set test-ids) (:id %)) tests)]
    (doseq [test tests]
      (run-test driver test opt))))

(defn run-ide-script
  [driver path & [{:keys [suite-ids test-ids] :as opt}]]
  (with-open [rdr (io/reader path)]
    (let [data             (parse-stream rdr true)
          {:keys [id
                  name
                  url
                  tests
                  suites]} data
          suite-ids        (when suite-ids
                             (set suite-ids))
          test-ids         (when test-ids
                             (set test-ids))
          opt              (merge {:base-url base-url
                                   :vars     (atom {})}
                                  opt)]
      (cond
        suite-ids (doseq [{:keys [name id] :as suite} suites]
                    (when (some suite-ids [name id])
                      (run-ide-suite driver suite tests opt)))

        test-ids (doseq [{:keys [name id] :as test} tests]
                   (when (some test-ids [name id])
                     (run-ide-test driver test opt)))

        :else (doseq [test tests]
                (run-test driver test opt))))))
