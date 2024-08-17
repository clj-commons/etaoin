(ns etaoin.test-report
  (:require [clojure.test :as t]))

(def ^:dynamic *context* nil)

(defmacro testing-with-report
  "Replacement for clojure.testing macro that hooks into reporting"
  [string & body]
  `(let [context# ~string]
     (binding [t/*testing-contexts* (conj t/*testing-contexts* context#)]
       (t/do-report {:type :begin-testing :testing-contexts t/*testing-contexts*})
       ~@body)))

;; Replace the clojure.test/testing macro with our custom one
(alter-var-root #'t/testing (constantly #'testing-with-report))

(def platform
  (if (System/getProperty "babashka.version") "bb" "jvm"))

(defmethod clojure.test/report :begin-test-var [m]
  (let [test-name (-> m :var meta :name)]
    (if *context*
      (println (format "=== %s [%s][%s]" test-name platform *context*))
      (println (format "=== %s [%s]" test-name platform)))))

(defmethod clojure.test/report :begin-testing [{:keys [testing-contexts]}]
  (println (format "...%s %s"
                   (apply str (repeat (count testing-contexts) "."))
                   (first testing-contexts))))
