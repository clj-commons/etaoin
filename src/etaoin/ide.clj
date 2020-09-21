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

;; TODO extend
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
  (let [[type val] (str/split value #"=" 2)
        q          (make-query target)]
    (case type
      "label" (select driver q val)

      "index" (let [index (inc (Integer/parseInt val))] ;; the initial index in selenium is 0, in xpath and css selectors it is 1
                (click driver q)
                (click-el driver (query driver q {:tag :option :index index})))

      (do
        (click driver q)
        (click-el driver (query driver q {:css (format "[%s=\"%s\"]" type val)}))))))

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

(defn get-tests-by-suite-id
  [suite-id id {:keys [suites tests]}]
  (let [test-ids    (-> (filter #(= suite-id (id %)) suites)
                        first
                        :tests
                        set)
        suite-tests (filter #(test-ids (:id %)) tests)]
    suite-tests))

(defn find-tests
  [{:keys [test-id test-ids suite-id suite-ids test-name suite-name]}
   {:keys [suites tests] :as parsed-file}]
  (let [test-ids    (cond-> #{}
                      test-id    (conj (first (filter #(= test-id (:id %)) tests)))
                      test-name  (conj (first (filter #(= test-id (:name %)) tests)))
                      suite-id   (into (get-tests-by-suite-id suite-id :id parsed-file))
                      suite-name (into (get-tests-by-suite-id suite-name :name parsed-file))
                      test-ids   (into (filter #((set test-ids) (:id %)) tests))
                      suite-ids  (into (mapcat #(get-tests-by-suite-id % :id parsed-file) suite-ids)))
        tests-found (filter test-ids tests)]
    (if (empty? tests-found)
      tests
      tests-found)))

(defn run-ide-script
  [driver path & [opt]]
  (let [parsed-file (with-open [rdr (io/reader path)]
                      (parse-stream rdr true))
        opt-search  (select-keys opt [:test-name :test-id :test-ids
                                      :suite-name :suite-id :suite-ids])
        tests-found (find-tests opt-search parsed-file)
        opt         (merge {:base-url (:url parsed-file)
                            :vars     (atom {})}
                           opt)]
    (doseq [test tests-found]
      (run-ide-test driver test opt))))
