(ns docker-entry
  ;; keep deps to built in to avoid having to bring over more sources to docker image
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]))

(defn copy-etaoin-sources
  "If the conditions are right, copy over mounted etaoin sources, if not warn."
  []
  ;; some sanity checks first
  (let [errors (reduce (fn [errors check]
                         (if-let [error (check)]
                           (conj errors error)
                           errors))
                       []
                       [#(when (not (fs/exists? "/etaoin"))
                           "/etaoin sources not mounted")
                        #(when (not (= "/home/etaoin-user/etaoin" (str (fs/cwd))))
                           "expected cwd to be /home/etaoin-user/etaoin")])]

    (if (seq errors)
      (do
        (println "* WARNING: etaoin sources not copied:")
        (run! #(println "-" %) errors))
      (do
        (println "copying mounted etaoin sources")
        (fs/copy-tree "/etaoin" ".")))))

(defn -main
  "Docker image entry point script to run at docker image launch.
  We cannot simply use a -v mounted /etaoin due to permissions, so we copy over
  the a mounted /etaoin to a spot where we do have full rights."
  [& args]
  (copy-etaoin-sources)
  ;; and now run provided command, else bash if none specified
  (let [cmd (if (seq args) args "/bin/bash")]
    (println "running command:" cmd)
    (proc/exec cmd)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
