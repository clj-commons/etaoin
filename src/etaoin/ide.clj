(ns etaoin.ide
  (:require [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as io]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (java.net URL)))

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

(defn dispatch-command
  [driver command & [opt]]
  (some-> command :command keyword))

(defmulti run-command dispatch-command)

(defmethod run-command
  :default
  [driver command & _]
  (log/warnf "The \"%s\" command is not implemented" (:command command)))

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
  [driver {:keys [target]} & [opt]]
  (let [[width height] (map #(Integer/parseInt %) (str/split target #"x"))]
    (set-window-size driver width height)))

(defmethod run-command
  :type
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) value))

(defmethod run-command
  :click
  [driver {:keys [target]} & [opt]]
  (click driver (make-query target)))

;; TODO refactor select fn, add select by-value
(defmethod run-command
  :select
  [driver {:keys [target value]} & [opt]]
  (let [[type & vals] (str/split value #"=")
        val           (str/join "=" vals)
        q             (make-query target)]
    (cond
      (= type "label") (select driver q value)

      (#{"id" "value"} type) (do
                               (click driver q)
                               (click-el driver (query driver q {:css (format "[%s=\"%s\"]" type val)})))

      (= type "index") (let [index (inc (Integer/parseInt val))] ;; the initial index in selenium is 0, in xpath and css selectors it is 1
                         (click driver q)
                         (click-el driver (query driver q {:tag :option :index index}))))))

;; TODO apply map to special-keys or auto-replace
(defmethod run-command
  :sendKeys
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) (get special-keys value)))

(defmethod run-command
  :click
  [driver {:keys [target]} & [opt]]
  (click driver (make-query target)))

(defn run-ide-test
  [driver {:keys [id commands]} & [opt]]
  (doseq [command commands]
    (run-command driver command opt)))

(defn run-ide-suite
  [driver {test-ids :tests} tests & [opt]]
  (let [tests (filter #((set test-ids) (:id %)) tests)]
    (doseq [test tests]
      (run-ide-test driver test opt))))

;; TODO make find-test fn
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
          opt              (merge {:base-url url
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
                (run-ide-test driver test opt))))))
