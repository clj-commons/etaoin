(ns etaoin.ide
  (:require [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as io]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [etaoin.util :refer [defmethods]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

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
  (let [[type val] (str/split target #"=" 2)]
    (case type
      "css"      {:css val}
      "xpath"    {:xpath val}
      "linkText" {:tag :a :fn/has-text val}
      {:css (format "[%s]" target)})))

(defn make-absolute-url
  [base-url target]
  (let [base-url (if (str/ends-with? base-url "/")
                   (subs base-url 0 (-> base-url count dec))
                   base-url)]
    (str base-url target)))

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
    (go driver (make-absolute-url base-url target))))

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

(defmethod run-command
  :close
  [driver _ & _]
  (close-window driver))

(defmethod run-command
  :doubleClick
  [driver {:keys [target]} & [opt]]
  (double-click driver (make-query target)))

(defmethod run-command
  :dragAndDropToObject
  [driver {:keys [target value]} & [opt]]
  (drag-and-drop driver
                 (make-query target)
                 (make-query value)))

(defmethod run-command
  :executeScript
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [result (js-execute driver target)]
    (when value
      (swap! vars assoc value result))
    result))

;; TODO refactor select fn, add select by-value
(defmethods run-command
  [:select :addSelection]
  [driver {:keys [target value]} & [opt]]
  (let [[type val] (str/split value #"=" 2)
        q          (make-query target)]
    (case type
      "label" (select driver q val)

      "index" (let [index (inc (Integer/parseInt val))] ;; the initial index in selenium is 0, in xpath and css selectors it is 1
                (click-el driver (query driver q {:tag :option :index index})))

      (click-el driver (query driver q (make-query value))))))

(defmethod run-command
  :selectFrame
  [driver {:keys [target]} & [opt]]
  (cond
    (= target "relative=top")          (switch-frame-top driver)
    (= target "relative=parent")       (switch-frame-parent driver)
    (str/starts-with? target "index=") (switch-frame* driver (-> target
                                                                 (str/split #"index=")
                                                                 second
                                                                 (Integer/parseInt)))
    :else                              (switch-frame driver (make-query target))))

;; TODO apply map to special-keys or auto-replace
(defmethod run-command
  :sendKeys
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) (get special-keys value)))

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
                      test-name  (conj (first (filter #(= test-name (:name %)) tests)))
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
