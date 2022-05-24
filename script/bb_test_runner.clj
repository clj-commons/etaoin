(ns bb-test-runner
  (:require [taoensso.timbre :as timer]
            [cognitect.test-runner :as test-runner]))

;; default log level for bb is debug, change it to info
(alter-var-root #'taoensso.timbre/*config* #(assoc % :min-level :info))

(defn -main [& args]
  (apply test-runner/-main args))
