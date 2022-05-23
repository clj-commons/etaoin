(ns bb-test-runner
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [babashka.tasks :as tasks]
            [helper.main :as main]))

(def id->nses {:ide ['etaoin.ide-test]
               :api ['etaoin.api-test]
               :unit (->> "test/etaoin/unit"
                           fs/list-dir
                           (map #(fs/relativize "test" %))
                           (map #(string/replace % #"\.clj$" ""))
                           (map #(string/replace % fs/file-separator "."))
                           (map #(string/replace % "_" "-"))
                           sort)})

(def args-usage "Valid args:
  (unit|api|ide|all)
  --help

Commands:
  unit Run only unit tests
  api  Run only api tests
  ide  Run only ide tests
  all  Run all tests

Options:
  --help  Show this help

Intended to be called from test.clj where setup for such things as browser
web browser selection and virtual displays occur")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [;; we only need bb-spec for ide and unit tests tests, so explicitly otherwise test without it
          cp-aliases (if (or (get opts "all") (get opts "ide") (get opts "unit"))
                       ":test:bb-spec"
                       ":test")
          nses (cond
                 (get opts "ide") (:ide id->nses)
                 (get opts "api") (:api id->nses)
                 (get opts "unit") (:unit id->nses)
                 (get opts "all") (mapcat second id->nses))
          runner (-> "script/bb_test_runner_template.clj"
                     slurp
                     (string/replace "{{cp-aliases}}" cp-aliases)
                     (string/replace "{{nses}}" (->> nses
                                                     (map #(str "'" %))
                                                     (string/join " "))))
          test-runner-file (-> (fs/create-temp-file {:prefix "bb-test-runner"
                                                     :suffix ".clj"})
                               fs/file)]
      (spit test-runner-file runner)
      (tasks/shell "bb" test-runner-file)
      (try
        (finally
          (fs/delete-if-exists test-runner-file))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
