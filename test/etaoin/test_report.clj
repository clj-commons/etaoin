(ns etaoin.test-report
  (:require [clojure.test]))

(def ^:dynamic *context*)

(defmethod clojure.test/report :begin-test-var [m]
  (let [test-name (-> m :var meta :name)]
    (if (bound? #'*context*)
      (println (format "=== %s [%s]" test-name *context*))
      (println (format "=== %s" test-name)))))
