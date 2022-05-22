(ns helper.shell
  (:require [babashka.tasks :as tasks]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [helper.os :as os]
            [lread.status-line :as status]))

(def default-opts {:error-fn
                   (fn die-on-error [{{:keys [exit cmd]} :proc}]
                     (status/die exit
                                 "exited with %d for: %s"
                                 exit
                                 (with-out-str (pprint/pprint cmd))))})

(defn command
  "Thin wrapper on babashka.tasks/shell that on error, prints status error message and exits.
  Launches everything through powershell if on windows (maybe not a good general solution (?) but
  ok for this project)."
  [cmd & args]
  (let [[opts cmd args] (if (map? cmd)
                          [cmd (first args) (rest args)]
                          [nil cmd args])
        opts (merge opts default-opts)]
    (if (= :win (os/get-os))
      (let [full-cmd (if (seq args)
                       ;; naive, but fine for our uses for now, adjust as necessary
                       (str cmd " " (string/join " " args))
                       cmd)]
        (tasks/shell opts "powershell" "-command"
                     ;; powershell -command does not automatically propagate exit code,
                     ;; hence the secret exit sauce here
                     (str full-cmd ";exit $LASTEXITCODE") ))
      (apply tasks/shell opts cmd args))))


(defn clojure
  "Wrap tasks/clojure for my loud error reporting treatment"
  [& args]
  (let [[opts args] (if (map? (first args))
                      [(first args) (rest args)]
                      [nil args])
        opts (merge opts default-opts)]
    (apply tasks/clojure opts args)))
