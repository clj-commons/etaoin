(ns etaoin.ide
  (:require [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as io]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]))


(defn dispatch-command
  [driver {command-name :command :as command} & [opt]]
  (if-not command-name
    (throw (ex-info "Command not found in test"
                    {:command command}))
    (keyword command-name)))

(defmulti run-command dispatch-command)

(defmethod run-command
  :default
  [driver command & _]
  (throw (ex-info "Command not implemented"
                  {:command command})))

(defn absolute-path?
  [path]
  (clojure.string/starts-with? path "http"))

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
    (go driver (str base-url target))))

(defmethod run-command
  :setWindowSize
  [driver {:keys [target]} & [{base-url :base-url}]])


(defmethod run-command
  :type
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) value))

(defmethod run-command
  :sendKeys
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) (get special-keys value)))

(defn run-test
  [driver {:keys [id commands]} & [opt]]
  (doseq [command commands]
    (run-command driver command opt)))

(defn run
  [driver path & [opt]]
  (with-open [rdr (io/reader path)]
    (let [data            (parse-stream rdr true)
          {:keys [id
                  name
                  url
                  tests]} data
          base-url        (or (:base-url opt)
                              url)
          opt             (assoc opt :base-url base-url)]
      (doseq [test tests]
        (run-test driver test opt)))))
