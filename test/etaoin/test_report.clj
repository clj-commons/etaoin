(ns etaoin.test-report
  (:require [clojure.test]))

(def ^:dynamic *context*)

(def platform
  (if (System/getProperty "babashka.version") "bb" "jvm"))

(defmethod clojure.test/report :begin-test-var [m]
  (let [test-name (-> m :var meta :name)]
    (if (bound? #'*context*)
      (println (format "=== %s [%s][%s]" test-name platform *context*))
      (println (format "=== %s [%s]" test-name platform)))))
