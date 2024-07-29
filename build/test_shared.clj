(ns test-shared
  (:refer-clojure :exclude [test])
  (:require [cognitect.test-runner :as tr]))

;; private fn pasted from original sources
(defn- do-test
  [{:keys [dirs nses patterns vars includes excludes]}]
  (let [adapted {:dir (when (seq dirs) (set dirs))
                 :namespace (when (seq nses) (set nses))
                 :namespace-regex (when (seq patterns) (map re-pattern patterns))
                 :var (when (seq vars) (set vars))
                 :include (when (seq includes) (set includes))
                 :exclude (when (seq excludes) (set excludes))}]
    (tr/test adapted)))

(defn test
  "Reimplement test to not throw but instead exit with 1 on error."
  [opts]
  (try
   (let [{:keys [fail error]} (do-test opts)]
     (System/exit (if (zero? (+ fail error)) 0 1)))
   (finally
     ;; Only called if `test` raises an exception
     (shutdown-agents))))
