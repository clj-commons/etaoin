(ns etaoin.test-report
  (:require [clojure.test]))

(def ^:dynamic *context* nil)

(def platform
  (if (System/getProperty "babashka.version") "bb" "jvm"))

(defmethod clojure.test/report :begin-test-var [m]
  (let [test-name (-> m :var meta :name)]
    (if *context*
      (println (format "=== %s [%s][%s]" test-name platform *context*))
      (println (format "=== %s [%s]" test-name platform)))))
