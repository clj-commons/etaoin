#!/usr/bin/env bb

(ns test-doc
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-doc-tests []
  (status/line :head "Generating tests for code blocks in documents")
  (shell/clojure "-X:test-doc-blocks gen-tests"))

(defn run-clj-doc-tests []
  (status/line :head "Running code block tests under Clojure")
  (shell/clojure "-M:test:test-docs" ))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (generate-doc-tests)
    (run-clj-doc-tests))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
