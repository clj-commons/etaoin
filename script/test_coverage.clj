(ns test-coverage
  (:require [babashka.fs :as fs]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-doc-tests []
  (status/line :head "Generating tests for code blocks in documents")
  (shell/command "clojure -X:test-doc-blocks gen-tests"))

(defn run-all-tests []
  (status/line :head "Running unit and code block tests under Clojure for coverage report")
  (shell/command "clojure" "-X:test:test-docs:clofidence"))

(defn -main [& _args]
  (fs/delete-tree "./target/clofidence")
  (generate-doc-tests)
  (run-all-tests))
