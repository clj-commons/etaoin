(require '[babashka.classpath :as cp]
         '[babashka.tasks :as tasks]
         '[clojure.test :as t]
         '[taoensso.timbre :as timbre])

;; bb log level by default is debug, let's set it to info
;; TODO: maybe there is some different abstraction for this?
(alter-var-root #'timbre/*config* #(assoc %1 :min-level :info))

(cp/add-classpath (with-out-str (tasks/clojure "-A{{cp-aliases}} -Spath")))

(require {{nses}})

(let [test-results (t/run-tests {{nses}})]
  (System/exit (+ (:fail test-results) (:error test-results))))
