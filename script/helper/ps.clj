(ns helper.ps
  ;; noice! bb is JDK 11 so we have ProcessHandle
  (:import (java.lang ProcessHandle)))

(defn all-processes []
  (for [p (-> (ProcessHandle/allProcesses) .iterator iterator-seq)
        :when (some-> p .info .command .isPresent)
        :let [info (.info p)
              command (-> info .command .get)
              arguments (when (-> info .arguments .isPresent)
                          (->> info .arguments .get (into [])))
              start-instant (-> info .startInstant .get)]]
    {:pid (.pid p)
     :start-instant start-instant
     :handle p
     :command command
     :arguments arguments}))
