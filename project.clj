(defproject etaoin "0.3.11-SNAPSHOT"

  :description "Pure Clojure Webdriver protocol implementation."

  :url "https://github.com/igrishaev/etaoin"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :deploy-repositories {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :release-tasks [["vcs" "assert-committed"]
                  ["shell" "make" "docker-test"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :profiles {:dev {:plugins      [[lein-codox "0.10.7"]]
                   :dependencies [[org.clojure/clojure "1.10.1"]
                                  [log4j/log4j "1.2.17"]]

                   :resource-paths ["env/dev/resources"]

                   :global-vars {*warn-on-reflection* true
                                 *assert*             true}}}

  :dependencies [[clj-http "3.10.1"]
                 [cheshire "5.9.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]]

  ;;
  ;; When running the tests as `lein test2junit`,
  ;; emit XUNIT test reports to enable CircleCI
  ;; to collect statistics over time
  ;;
  :plugins [[test2junit "1.1.2"]
            [lein-shell "0.5.0"]]
  :test2junit-output-dir "target/test2junit"

  :codox {:output-path "autodoc"})
