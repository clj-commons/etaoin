(ns lint
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def clj-kondo-cache ".clj-kondo/.cache")

(defn- cache-exists? []
  (fs/exists? clj-kondo-cache))

(defn- delete-cache []
  (when (cache-exists?)
    (fs/delete-tree clj-kondo-cache)))

(defn- build-cache []
  (when (cache-exists?)
    (delete-cache))
  (let [clj-cp (-> (shell/clojure {:out :string}
                                  "-Spath -M:test" )
                   with-out-str
                   string/trim)
        bb-cp (-> (shell/command {:out :string}
                                 "bb print-deps --format classpath")
                  :out
                  string/trim)]
    
    (status/line :detail "- copying lib configs and creating cache")
    (shell/clojure "-M:clj-kondo --parallel --skip-lint --copy-configs --dependencies --lint" clj-cp bb-cp)))

(defn- check-cache [{:keys [rebuild-cache]}]
  (status/line :head "clj-kondo: cache check")
  (if-let [rebuild-reason (cond
                            rebuild-cache
                            "Rebuild requested"

                            (not (cache-exists?))
                            "Cache not found"

                            :else
                            (let [updated-dep-files (fs/modified-since clj-kondo-cache ["deps.edn" "bb.edn"])]
                              (when (seq updated-dep-files)
                                (format "Found deps files newer than lint cache: %s" (mapv str updated-dep-files)))))]
    (do (status/line :detail rebuild-reason)
        (build-cache))
    (status/line :detail "Using existing cache")))

(defn- lint [opts]
  (check-cache opts)
  (status/line :head "clj-kondo: linting")
  (let [{:keys [exit]}
        (shell/clojure {:continue true}
                       "-M:clj-kondo --parallel --lint src test script env deps.edn bb.edn")]
    (cond
      (= 2 exit) (status/die exit "clj-kondo found one or more lint errors")
      (= 3 exit) (status/die exit "clj-kondo found one or more lint warnings")
      (> exit 0) (status/die exit "clj-kondo returned unexpected exit code"))))

(def args-usage "Valid args: [options]

Options:
  --rebuild   Force rebuild of clj-kondo lint cache and check for config imports.
  --help      Show this help.")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (lint {:rebuild-cache (get opts "--rebuild")})))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
