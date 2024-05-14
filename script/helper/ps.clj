(ns helper.ps
  ;; noice! bb uses a modern JDK so we have ProcessHandle
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
     :is-alive (.isAlive p)
     :start-instant start-instant
     :handle p
     :command command
     :arguments arguments}))
